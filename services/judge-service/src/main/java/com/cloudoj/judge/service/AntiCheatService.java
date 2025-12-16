package com.cloudoj.judge.service;

import com.cloudoj.model.entity.problem.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 防作弊服务
 * 检测硬编码输出、代码相似度等作弊行为
 */
@Slf4j
@Service
public class AntiCheatService {
    
    /**
     * 检测代码是否存在硬编码输出作弊
     * 
     * @param code 用户提交的代码
     * @param language 编程语言
     * @param testCases 测试用例列表
     * @return 作弊检测结果
     */
    public CheatDetectionResult detectHardcodedOutput(String code, String language, List<TestCase> testCases) {
        CheatDetectionResult result = new CheatDetectionResult();
        result.setCheatDetected(false);
        
        if (code == null || testCases == null || testCases.isEmpty()) {
            return result;
        }
        
        // 检查是否所有测试用例都没有输入（如 Hello World 类题目）
        boolean hasNoInput = testCases.stream()
                .allMatch(tc -> tc.getInput() == null || tc.getInput().trim().isEmpty());
        
        // 如果题目本身没有输入，跳过输入相关检测
        if (hasNoInput) {
            log.debug("题目无输入要求，跳过输入检测");
            return result;
        }
        
        // 1. 检查代码是否读取了输入
        boolean readsInput = checkIfCodeReadsInput(code, language);
        if (!readsInput) {
            // 代码完全没有读取输入，直接判定为作弊
            result.setCheatDetected(true);
            result.setCheatType("NO_INPUT_READ");
            result.setMessage("代码未读取任何输入数据，疑似硬编码输出");
            log.warn("检测到作弊：代码未读取输入");
            return result;
        }
        
        // 2. 检查代码是否使用了输入（不仅仅是读取后丢弃）
        boolean usesInput = checkIfCodeUsesInput(code, language);
        if (!usesInput) {
            result.setCheatDetected(true);
            result.setCheatType("INPUT_NOT_USED");
            result.setMessage("代码读取了输入但未使用，疑似硬编码输出");
            log.warn("检测到作弊：代码读取输入但未使用");
            return result;
        }
        
        // 3. 检查代码复杂度
        int codeComplexity = calculateCodeComplexity(code, language);
        int codeLines = countEffectiveLines(code);
        
        // 4. 提取代码中的所有字符串字面量
        Set<String> codeStrings = extractStringLiterals(code, language);
        
        // 5. 检查是否存在条件分支硬编码（如 if input == "xxx" then print "yyy"）
        if (detectConditionalHardcode(code, language, testCases)) {
            result.setCheatDetected(true);
            result.setCheatType("CONDITIONAL_HARDCODE");
            result.setMessage("检测到条件分支硬编码：根据输入直接映射输出");
            log.warn("检测到条件分支硬编码作弊");
            return result;
        }
        
        // 6. 代码过于简单的检测（针对简单题目如两数之和）
        // 如果代码行数很少且没有实质性的计算逻辑
        if (codeLines < 10 && codeComplexity < 2) {
            // 检查是否只是简单打印
            if (isSimplePrintOnly(code, language)) {
                result.setCheatDetected(true);
                result.setCheatType("SIMPLE_PRINT");
                result.setMessage("代码过于简单，仅包含打印语句");
                log.warn("检测到简单打印作弊");
                return result;
            }
        }
        
        // 7. 检查输出是否与测试用例高度匹配（原有逻辑，作为补充）
        // 如果代码有计算逻辑，跳过此检测（避免误判如奇偶判断等简单题目）
        if (hasCalculation(code)) {
            log.debug("代码包含计算逻辑，跳过硬编码输出检测");
            return result;
        }
        
        Set<String> codeNumbers = extractNumberLiterals(code);
        int matchedOutputCount = 0;
        List<String> matchedOutputs = new ArrayList<>();
        
        // 统计不同的输出数量
        Set<String> uniqueOutputs = new HashSet<>();
        for (TestCase testCase : testCases) {
            uniqueOutputs.add(testCase.getOutput().trim());
        }
        
        // 如果输出种类很少（如只有2种：Even/Odd, Yes/No等），跳过检测
        // 因为这类题目代码中必须包含这些输出字符串
        if (uniqueOutputs.size() <= 3) {
            log.debug("输出种类较少({}种)，跳过硬编码检测", uniqueOutputs.size());
            return result;
        }
        
        // 只有当输出多样性足够时才进行硬编码检测
        if (uniqueOutputs.size() >= testCases.size() * 0.5) {
            for (TestCase testCase : testCases) {
                String expectedOutput = testCase.getOutput().trim();
                
                if (containsOutput(codeStrings, expectedOutput)) {
                    matchedOutputCount++;
                    matchedOutputs.add(expectedOutput);
                    continue;
                }
                
                String[] outputParts = expectedOutput.split("\\s+");
                int matchedParts = 0;
                for (String part : outputParts) {
                    if (codeNumbers.contains(part) || codeStrings.contains(part)) {
                        matchedParts++;
                    }
                }
                
                if (outputParts.length > 0 && (double) matchedParts / outputParts.length > 0.8) {
                    matchedOutputCount++;
                    matchedOutputs.add(expectedOutput);
                }
            }
            
            double cheatRatio = (double) matchedOutputCount / testCases.size();
            if (cheatRatio >= 0.8 && testCases.size() > 1) {
                result.setCheatDetected(true);
                result.setCheatType("HARDCODED_OUTPUT");
                result.setMessage("检测到硬编码输出作弊：代码中包含大量测试用例的预期输出");
                result.setMatchedOutputs(matchedOutputs);
                log.warn("检测到硬编码输出作弊: matchedRatio={}", cheatRatio);
            }
        }
        
        return result;
    }
    
    /**
     * 检查代码是否读取了输入
     * 注意：仅检查 main 方法签名中的 args 不算读取输入，必须实际使用 args
     */
    private boolean checkIfCodeReadsInput(String code, String language) {
        if ("JAVA".equalsIgnoreCase(language)) {
            // Java: Scanner, BufferedReader, System.in
            // 注意：args 必须实际使用（args[x] 或 args.length），不能只是声明
            boolean usesScanner = code.contains("Scanner") && code.contains("System.in");
            boolean usesBufferedReader = code.contains("BufferedReader");
            boolean usesSystemIn = code.contains("System.in") && !code.contains("System.out");
            // args 必须实际访问，不能只是 main(String[] args) 声明
            boolean usesArgs = code.contains("args[") || 
                              (code.contains("args.length") && code.contains("args"));
            
            return usesScanner || usesBufferedReader || usesSystemIn || usesArgs;
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            // Python: input(), sys.stdin, raw_input()
            return code.contains("input(") || 
                   code.contains("sys.stdin") ||
                   code.contains("raw_input(");
        } else if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            // C/C++: scanf, cin, gets, fgets, getchar
            return code.contains("scanf") || 
                   code.contains("cin") ||
                   code.contains("gets(") ||
                   code.contains("fgets(") ||
                   code.contains("getchar(") ||
                   (code.contains("argc") && code.contains("argv"));
        }
        return true; // 未知语言默认通过
    }
    
    /**
     * 检查代码是否实际使用了输入（不仅仅是读取后丢弃）
     */
    private boolean checkIfCodeUsesInput(String code, String language) {
        if ("JAVA".equalsIgnoreCase(language)) {
            // 检查是否有变量接收输入（多种形式）
            // 形式1: int a = sc.nextInt();
            Pattern pattern1 = Pattern.compile("\\w+\\s+\\w+\\s*=\\s*\\w+\\.(next|read)");
            if (pattern1.matcher(code).find()) return true;
            // 形式2: a = sc.nextInt();
            Pattern pattern2 = Pattern.compile("\\w+\\s*=\\s*\\w+\\.(next|read)");
            if (pattern2.matcher(code).find()) return true;
            // 形式3: Integer.parseInt, Long.parseLong 等
            Pattern pattern3 = Pattern.compile("\\w+\\s*=\\s*(Integer|Long|Double|Float)\\.(parse|value)");
            if (pattern3.matcher(code).find()) return true;
            // 形式4: 直接在表达式中使用 sc.nextInt() + sc.nextInt()
            Pattern pattern4 = Pattern.compile("\\w+\\.(nextInt|nextLong|nextDouble|nextLine)\\s*\\(\\s*\\)\\s*[+\\-*/]");
            if (pattern4.matcher(code).find()) return true;
            // 形式5: System.out.println(sc.nextInt() + sc.nextInt())
            Pattern pattern5 = Pattern.compile("print.*\\(.*\\w+\\.(next|read)");
            if (pattern5.matcher(code).find()) return true;
            // 形式6: 变量在输出中使用（如 n % 2, a + b 等）
            // 检查是否有变量赋值后在后续代码中使用
            Pattern varAssign = Pattern.compile("(int|long|double|float|String)\\s+(\\w+)\\s*=");
            Matcher varMatcher = varAssign.matcher(code);
            while (varMatcher.find()) {
                String varName = varMatcher.group(2);
                // 检查变量是否在输出语句中使用
                if (code.contains("System.out") && code.contains(varName) && 
                    code.indexOf(varName, varMatcher.end()) > varMatcher.end()) {
                    return true;
                }
            }
            return false;
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            // 检查是否有变量接收 input() 或直接使用
            Pattern pattern = Pattern.compile("(\\w+\\s*=\\s*.*input\\(|int\\s*\\(\\s*input|float\\s*\\(\\s*input|print\\s*\\(.*input)");
            return pattern.matcher(code).find();
        } else if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            // 检查 scanf 是否有变量接收
            Pattern pattern = Pattern.compile("scanf\\s*\\([^,]+,\\s*&?\\w+");
            if (pattern.matcher(code).find()) return true;
            // 检查 cin >> 变量
            Pattern cinPattern = Pattern.compile("cin\\s*>>\\s*\\w+");
            return cinPattern.matcher(code).find();
        }
        return true;
    }
    
    /**
     * 检测条件分支硬编码（if input == xxx then print yyy）
     */
    private boolean detectConditionalHardcode(String code, String language, List<TestCase> testCases) {
        // 统计代码中的条件分支数量
        int ifCount = 0;
        Pattern ifPattern = Pattern.compile("\\bif\\b");
        Matcher matcher = ifPattern.matcher(code);
        while (matcher.find()) {
            ifCount++;
        }
        
        // 如果条件分支数量接近测试用例数量，可能是硬编码
        if (ifCount >= testCases.size() - 1 && testCases.size() > 2) {
            // 进一步检查：条件中是否包含测试用例的输入
            int matchedInputCount = 0;
            for (TestCase testCase : testCases) {
                String input = testCase.getInput().trim();
                // 检查输入的关键部分是否出现在代码中
                String[] inputParts = input.split("\\s+");
                for (String part : inputParts) {
                    if (part.length() > 2 && code.contains(part)) {
                        matchedInputCount++;
                        break;
                    }
                }
            }
            
            // 如果大部分输入都出现在代码中，判定为条件硬编码
            return matchedInputCount >= testCases.size() * 0.6;
        }
        
        return false;
    }
    
    /**
     * 检查代码是否只是简单的打印语句（没有任何计算逻辑）
     */
    private boolean isSimplePrintOnly(String code, String language) {
        // 如果代码中包含计算操作符，说明有计算逻辑，不是简单打印
        if (hasCalculation(code)) {
            return false;
        }
        
        // 如果代码中读取了输入并在打印中使用，说明有处理逻辑
        if (hasInputInOutput(code, language)) {
            return false;
        }
        
        // 移除注释和空行后检查
        String cleanCode = removeComments(code, language);
        String[] lines = cleanCode.split("\n");
        
        int printCount = 0;
        int otherCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 跳过导入语句、类定义、方法定义等
            if (isBoilerplateLine(line, language)) continue;
            
            // 跳过输入语句
            if (isInputStatement(line, language)) continue;
            
            // 检查是否是打印语句
            if (isPrintStatement(line, language)) {
                printCount++;
            } else {
                otherCount++;
            }
        }
        
        // 如果打印语句占主导，且没有实质性的其他代码，且打印的是硬编码字符串
        return printCount > 0 && otherCount <= 1 && !hasInputInOutput(code, language);
    }
    
    /**
     * 检查代码是否包含计算操作
     */
    private boolean hasCalculation(String code) {
        // 检查是否有算术运算（排除字符串中的）
        // 匹配变量或函数调用后跟运算符
        Pattern calcPattern = Pattern.compile("\\)\\s*[+\\-*/%]|[+\\-*/%]\\s*\\w+\\.|\\w+\\s*[+\\-*/%]\\s*\\w+");
        if (calcPattern.matcher(code).find()) return true;
        
        // 检查取模运算 (如 n % 2)
        Pattern modPattern = Pattern.compile("\\w+\\s*%\\s*\\d+");
        if (modPattern.matcher(code).find()) return true;
        
        // 检查比较运算 (如 n == 0, a > b)
        Pattern comparePattern = Pattern.compile("\\w+\\s*(==|!=|>=|<=|>|<)\\s*\\w+");
        if (comparePattern.matcher(code).find()) return true;
        
        // 检查三元运算符
        if (code.contains("?") && code.contains(":")) return true;
        
        return false;
    }
    
    /**
     * 检查输出语句中是否使用了输入或变量
     */
    private boolean hasInputInOutput(String code, String language) {
        if ("JAVA".equalsIgnoreCase(language)) {
            // 检查 System.out.println 中是否包含输入相关调用
            Pattern pattern1 = Pattern.compile("System\\.out\\.print.*\\(.*\\.(next|read)");
            if (pattern1.matcher(code).find()) return true;
            
            // 检查是否在输出中使用了变量（包括三元运算符等）
            // 先找到所有定义的变量
            Pattern varDefPattern = Pattern.compile("(int|long|double|float|String|char|boolean)\\s+(\\w+)\\s*=");
            Matcher varMatcher = varDefPattern.matcher(code);
            Set<String> definedVars = new HashSet<>();
            while (varMatcher.find()) {
                definedVars.add(varMatcher.group(2));
            }
            
            // 检查输出语句中是否使用了这些变量
            Pattern printPattern = Pattern.compile("System\\.out\\.print[^;]+");
            Matcher printMatcher = printPattern.matcher(code);
            while (printMatcher.find()) {
                String printStmt = printMatcher.group();
                for (String var : definedVars) {
                    // 检查变量是否在打印语句中使用（不在引号内）
                    if (printStmt.contains(var)) {
                        // 简单检查：变量后面跟着运算符或空格或括号
                        Pattern varUsePattern = Pattern.compile("\\b" + var + "\\b(?![\"'])");
                        if (varUsePattern.matcher(printStmt).find()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            Pattern pattern = Pattern.compile("print\\s*\\(.*input|print\\s*\\([^\"']*[a-zA-Z_]");
            return pattern.matcher(code).find();
        } else if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            // C/C++ 通常先读取再输出，检查是否有变量在输出中使用
            return true; // 默认认为有使用
        }
        return false;
    }
    
    /**
     * 检查是否是输入语句
     */
    private boolean isInputStatement(String line, String language) {
        if ("JAVA".equalsIgnoreCase(language)) {
            return line.contains("Scanner") || line.contains(".next") || line.contains(".read");
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            return line.contains("input(");
        } else if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            return line.contains("scanf") || line.contains("cin");
        }
        return false;
    }
    
    /**
     * 检查是否是模板代码行（导入、类定义等）
     */
    private boolean isBoilerplateLine(String line, String language) {
        if ("JAVA".equalsIgnoreCase(language)) {
            return line.startsWith("import ") || 
                   line.startsWith("package ") ||
                   line.startsWith("public class") ||
                   line.startsWith("class ") ||
                   line.startsWith("public static void main") ||
                   line.equals("{") || line.equals("}");
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            return line.startsWith("import ") || 
                   line.startsWith("from ") ||
                   line.startsWith("def ") ||
                   line.startsWith("if __name__");
        } else if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            return line.startsWith("#include") || 
                   line.startsWith("using namespace") ||
                   line.startsWith("int main") ||
                   line.equals("{") || line.equals("}");
        }
        return false;
    }
    
    /**
     * 检查是否是打印语句
     */
    private boolean isPrintStatement(String line, String language) {
        if ("JAVA".equalsIgnoreCase(language)) {
            return line.contains("System.out.print");
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            return line.startsWith("print(") || line.startsWith("print ");
        } else if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            return line.contains("printf") || line.contains("cout");
        }
        return false;
    }
    
    /**
     * 移除代码中的注释
     */
    private String removeComments(String code, String language) {
        // 移除单行注释
        code = code.replaceAll("//.*", "");
        // 移除多行注释
        code = code.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
        // Python 注释
        if ("PYTHON".equalsIgnoreCase(language)) {
            code = code.replaceAll("#.*", "");
        }
        return code;
    }
    
    /**
     * 统计有效代码行数
     */
    private int countEffectiveLines(String code) {
        String[] lines = code.split("\n");
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("#")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 提取代码中的字符串字面量
     */
    private Set<String> extractStringLiterals(String code, String language) {
        Set<String> strings = new HashSet<>();
        
        // 匹配双引号字符串
        Pattern doubleQuotePattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = doubleQuotePattern.matcher(code);
        while (matcher.find()) {
            strings.add(matcher.group(1).trim());
        }
        
        // 匹配单引号字符串（Python等）
        if ("PYTHON".equalsIgnoreCase(language)) {
            Pattern singleQuotePattern = Pattern.compile("'([^']*)'");
            matcher = singleQuotePattern.matcher(code);
            while (matcher.find()) {
                String str = matcher.group(1).trim();
                if (str.length() > 1) { // 排除单字符
                    strings.add(str);
                }
            }
        }
        
        return strings;
    }
    
    /**
     * 提取代码中的数字字面量
     */
    private Set<String> extractNumberLiterals(String code) {
        Set<String> numbers = new HashSet<>();
        
        // 匹配整数和浮点数
        Pattern numberPattern = Pattern.compile("\\b(-?\\d+\\.?\\d*)\\b");
        Matcher matcher = numberPattern.matcher(code);
        while (matcher.find()) {
            String num = matcher.group(1);
            // 排除常见的非作弊数字（0, 1, 10, 100等）
            if (!isCommonNumber(num)) {
                numbers.add(num);
            }
        }
        
        return numbers;
    }
    
    /**
     * 判断是否为常见的非作弊数字
     */
    private boolean isCommonNumber(String num) {
        Set<String> commonNumbers = Set.of(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "100", "1000", "10000", "100000", "1000000",
            "-1", "0.0", "1.0", "0.5"
        );
        return commonNumbers.contains(num);
    }
    
    /**
     * 检查代码字符串是否包含预期输出
     */
    private boolean containsOutput(Set<String> codeStrings, String expectedOutput) {
        // 完全匹配
        if (codeStrings.contains(expectedOutput)) {
            return true;
        }
        
        // 检查输出是否被拆分成多个字符串
        for (String codeStr : codeStrings) {
            if (expectedOutput.contains(codeStr) && codeStr.length() > 3) {
                // 如果代码字符串是输出的一部分，且长度足够
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算代码复杂度（简化版）
     * 基于控制结构数量
     */
    private int calculateCodeComplexity(String code, String language) {
        int complexity = 0;
        
        // 统计控制结构
        String[] controlStructures = {"if", "else", "for", "while", "switch", "case", "try", "catch"};
        for (String structure : controlStructures) {
            Pattern pattern = Pattern.compile("\\b" + structure + "\\b");
            Matcher matcher = pattern.matcher(code);
            while (matcher.find()) {
                complexity++;
            }
        }
        
        // 统计三元运算符（等价于 if-else）
        Pattern ternaryPattern = Pattern.compile("\\?[^?:]+:");
        Matcher ternaryMatcher = ternaryPattern.matcher(code);
        while (ternaryMatcher.find()) {
            complexity += 2; // 三元运算符等价于 if-else
        }
        
        // 统计算术/逻辑运算（表示有计算逻辑）
        Pattern calcPattern = Pattern.compile("\\w+\\s*[+\\-*/%]\\s*\\w+|\\w+\\s*(==|!=|>=|<=|>|<)\\s*\\w+");
        Matcher calcMatcher = calcPattern.matcher(code);
        while (calcMatcher.find()) {
            complexity++;
        }
        
        // 统计函数/方法定义
        if ("JAVA".equalsIgnoreCase(language)) {
            Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{");
            Matcher matcher = methodPattern.matcher(code);
            while (matcher.find()) {
                complexity++;
            }
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            Pattern defPattern = Pattern.compile("\\bdef\\s+\\w+\\s*\\(");
            Matcher matcher = defPattern.matcher(code);
            while (matcher.find()) {
                complexity++;
            }
        }
        
        return complexity;
    }
    
    /**
     * 代码安全检查 - 检测危险的系统调用和操作
     * @param code 用户提交的代码
     * @param language 编程语言
     * @return 安全检查结果，如果不安全返回错误信息，安全返回null
     */
    public String checkCodeSecurity(String code, String language) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        
        if ("C".equalsIgnoreCase(language) || "CPP".equalsIgnoreCase(language)) {
            return checkCCodeSecurity(code);
        } else if ("JAVA".equalsIgnoreCase(language)) {
            return checkJavaCodeSecurity(code);
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            return checkPythonCodeSecurity(code);
        }
        
        return null;
    }
    
    /**
     * C/C++ 代码安全检查
     */
    private String checkCCodeSecurity(String code) {
        // 危险的系统调用
        String[] dangerousFunctions = {
            "system", "popen", "exec", "fork", "vfork",
            "kill", "signal", "raise",
            "setuid", "setgid", "seteuid", "setegid",
            "chroot", "chdir", "chmod", "chown",
            "unlink", "remove", "rmdir", "rename",
            "socket", "connect", "bind", "listen", "accept",
            "sendto", "recvfrom", "send", "recv",
            "mmap", "munmap", "mprotect",
            "ptrace", "prctl",
            "dlopen", "dlsym"
        };
        
        for (String func : dangerousFunctions) {
            // 匹配函数调用（函数名后跟括号）
            Pattern pattern = Pattern.compile("\\b" + func + "\\s*\\(");
            if (pattern.matcher(code).find()) {
                log.warn("C/C++代码安全检查失败: 检测到危险函数 {}", func);
                return "代码包含禁止使用的系统函数: " + func;
            }
        }
        
        // 危险的头文件
        String[] dangerousHeaders = {
            "sys/socket.h", "netinet/in.h", "arpa/inet.h",
            "sys/ptrace.h", "dlfcn.h"
        };
        
        for (String header : dangerousHeaders) {
            if (code.contains("#include") && code.contains(header)) {
                log.warn("C/C++代码安全检查失败: 检测到危险头文件 {}", header);
                return "代码包含禁止使用的头文件: " + header;
            }
        }
        
        // 检查内联汇编
        if (code.contains("__asm") || code.contains("asm(") || code.contains("asm volatile")) {
            log.warn("C/C++代码安全检查失败: 检测到内联汇编");
            return "代码包含禁止使用的内联汇编";
        }
        
        return null;
    }
    
    /**
     * Java 代码安全检查
     */
    private String checkJavaCodeSecurity(String code) {
        // 危险的类和方法
        String[] dangerousPatterns = {
            "Runtime.getRuntime",
            "ProcessBuilder",
            "System.exit",
            "SecurityManager",
            "ClassLoader",
            "java.io.File",  // 文件操作需要谨慎
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.URL",
            "java.lang.reflect",
            "sun.misc.Unsafe"
        };
        
        for (String pattern : dangerousPatterns) {
            if (code.contains(pattern)) {
                // 允许 File 用于某些场景，但禁止删除等操作
                if (pattern.equals("java.io.File")) {
                    if (code.contains(".delete(") || code.contains(".deleteOnExit(") ||
                        code.contains(".mkdir(") || code.contains(".createNewFile(")) {
                        log.warn("Java代码安全检查失败: 检测到危险文件操作");
                        return "代码包含禁止的文件操作";
                    }
                    continue;
                }
                log.warn("Java代码安全检查失败: 检测到危险模式 {}", pattern);
                return "代码包含禁止使用的类或方法: " + pattern;
            }
        }
        
        return null;
    }
    
    /**
     * Python 代码安全检查
     */
    private String checkPythonCodeSecurity(String code) {
        // 危险的模块和函数
        String[] dangerousPatterns = {
            "import os",
            "from os",
            "import subprocess",
            "from subprocess",
            "import socket",
            "from socket",
            "import shutil",
            "from shutil",
            "__import__",
            "eval(",
            "exec(",
            "compile(",
            "open(",  // 文件操作
            "os.system",
            "os.popen",
            "os.exec",
            "os.spawn",
            "os.fork",
            "os.kill",
            "os.remove",
            "os.unlink",
            "os.rmdir",
            "subprocess.call",
            "subprocess.run",
            "subprocess.Popen"
        };
        
        for (String pattern : dangerousPatterns) {
            if (code.contains(pattern)) {
                // open() 用于读取输入是允许的，但需要检查模式
                if (pattern.equals("open(")) {
                    // 只允许读取模式
                    Pattern writePattern = Pattern.compile("open\\s*\\([^)]*['\"][wa]['\"]");
                    if (writePattern.matcher(code).find()) {
                        log.warn("Python代码安全检查失败: 检测到文件写入操作");
                        return "代码包含禁止的文件写入操作";
                    }
                    continue;
                }
                log.warn("Python代码安全检查失败: 检测到危险模式 {}", pattern);
                return "代码包含禁止使用的模块或函数: " + pattern;
            }
        }
        
        return null;
    }
    
    /**
     * 作弊检测结果
     */
    @lombok.Data
    public static class CheatDetectionResult {
        /**
         * 是否检测到作弊
         */
        private boolean cheatDetected;
        
        /**
         * 是否可疑（未确定作弊但有嫌疑）
         */
        private boolean suspicious;
        
        /**
         * 作弊类型
         */
        private String cheatType;
        
        /**
         * 提示信息
         */
        private String message;
        
        /**
         * 匹配到的输出
         */
        private List<String> matchedOutputs;
    }
}
