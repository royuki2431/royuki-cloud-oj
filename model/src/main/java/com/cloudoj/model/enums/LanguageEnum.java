package com.cloudoj.model.enums;

import lombok.Getter;

/**
 * 编程语言枚举
 */
@Getter
public enum LanguageEnum {
    
    JAVA("JAVA", "Java", "java", "javac"),
    C("C", "C", "c", "gcc"),
    CPP("CPP", "C++", "cpp", "g++"),
    PYTHON("PYTHON", "Python", "py", "python3");
    
    private final String code;
    private final String name;
    private final String extension;
    private final String compiler;
    
    LanguageEnum(String code, String name, String extension, String compiler) {
        this.code = code;
        this.name = name;
        this.extension = extension;
        this.compiler = compiler;
    }
    
    public static LanguageEnum getByCode(String code) {
        for (LanguageEnum language : values()) {
            if (language.getCode().equals(code)) {
                return language;
            }
        }
        return null;
    }
}
