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
        Set<String> codeNumbers = extractNumberLiterals(code);
        int matchedOutputCount = 0;
        List<String> matchedOutputs = new ArrayList<>();
        
        // 统计不同的输出数量
        Set<String> uniqueOutputs = new HashSet<>();
        for (TestCase testCase : testCases) {
            uniqueOutputs.add(testCase.getOutput().trim());
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
            // 检查是否有变量接收输入
            Pattern pattern = Pattern.compile("(\\w+)\\s*=\\s*\\w+\\.(next|read|parseInt|parseLong)");
            return pattern.matcher(code).find();
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            // 检查是否有变量接收 input()
            Pattern pattern = Pattern.compile("(\\w+)\\s*=\\s*.*input\\(");
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
     * 检查代码是否只是简单的打印语句
     */
    private boolean isSimplePrintOnly(String code, String language) {
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
            
            // 检查是否是打印语句
            if (isPrintStatement(line, language)) {
                printCount++;
            } else {
                otherCount++;
            }
        }
        
        // 如果打印语句占主导，且没有实质性的其他代码
        return printCount > 0 && otherCount <= 2;
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
