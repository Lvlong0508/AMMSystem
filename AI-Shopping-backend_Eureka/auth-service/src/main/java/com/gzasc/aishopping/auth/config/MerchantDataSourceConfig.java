package com.gzasc.aishopping.auth.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 商家数据源配置
 * 扫描 MerchantMapper 接口
 */
@Configuration
@MapperScan(basePackages = "com.gzasc.aishopping.auth.mapper.merchant", 
             sqlSessionFactoryRef = "merchantSqlSessionFactory")
public class MerchantDataSourceConfig {

    @Bean(name = "merchantSqlSessionFactory")
    public SqlSessionFactory merchantSqlSessionFactory(@Qualifier("merchantDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/merchant/*.xml"));
        bean.setTypeAliasesPackage("com.gzasc.aishopping.auth.model");
        return bean.getObject();
    }

    @Bean(name = "merchantSqlSessionTemplate")
    public SqlSessionTemplate merchantSqlSessionTemplate(@Qualifier("merchantSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
