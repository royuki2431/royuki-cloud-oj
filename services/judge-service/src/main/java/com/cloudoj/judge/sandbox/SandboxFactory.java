package com.cloudoj.judge.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 沙箱工厂类
 * 根据编程语言选择对应的沙箱实现
 */
@Slf4j
@Component
public class SandboxFactory {
    
    @Autowired
    private JavaSandbox javaSandbox;
    
    @Autowired
    private CppSandbox cppSandbox;
    
    @Autowired
    private CSandbox cSandbox;
    
    @Autowired
    private PythonSandbox pythonSandbox;
    
    /**
     * 根据语言获取对应的沙箱
     * 
     * @param language 编程语言（JAVA/CPP/C/PYTHON）
     * @return 语言沙箱实例
     */
    public LanguageSandbox getSandbox(String language) {
        if (language == null) {
            throw new IllegalArgumentException("编程语言不能为空");
        }
        
        switch (language.toUpperCase()) {
            case "JAVA":
                log.debug("使用Java沙箱");
                return javaSandbox;
            case "CPP":
            case "C++":
                log.debug("使用C++沙箱");
                return cppSandbox;
            case "C":
                log.debug("使用C沙箱");
                return cSandbox;
            case "PYTHON":
            case "PYTHON3":
                log.debug("使用Python沙箱");
                return pythonSandbox;
            default:
                throw new IllegalArgumentException("不支持的编程语言: " + language);
        }
    }
}
