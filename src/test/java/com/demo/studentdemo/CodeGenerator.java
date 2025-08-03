package com.demo.studentdemo;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.nio.file.Paths;

public class CodeGenerator {
    public static void main(String[] args) {
        //支持多个表名，使用,隔开
        String tableName = "t_course";

        FastAutoGenerator.create("jdbc:mysql://localhost:3306/student_demo", "root", "dxlove")
                .globalConfig(builder -> builder
                        .author("admin")
                        .outputDir(Paths.get(System.getProperty("user.dir")) + "/src/main/java")
                        //.enableSwagger() // 开启 swagger 模式
                        .enableSpringdoc()
                        .commentDate("yyyy-MM-dd")
                )
                .packageConfig(builder -> builder
                        .parent("com.demo.studentdemo")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper.xml")
                )
                .strategyConfig(builder -> builder
                        .entityBuilder()
                        .enableLombok()
                )
                .strategyConfig(builder ->
                        builder.addInclude(tableName) // 设置需要生成的表名
                                .addTablePrefix("t_", "c_") // 设置过滤表前缀
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
