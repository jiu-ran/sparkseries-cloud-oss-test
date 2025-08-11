package com.sparkseries.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring Bean 工具类
 * 用于动态注册 销毁  获取 Bean
 */
@Slf4j
@Component
public class SpringBeanUtil implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private ConfigurableListableBeanFactory beanFactory;

    /**
     * 设置 ApplicationContext
     *
     * @param applicationContext 应用上下文
     * @throws BeansException Beans 异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        this.beanFactory = configurableApplicationContext.getBeanFactory();
    }

    /**
     * 根据名称获取 Bean
     *
     * @param name Bean 名称
     * @return Bean
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) throws BeansException {
        return (T) applicationContext.getBean(name);
    }

    /**
     * 动态注册一个 Bean (带构造函数参数)
     *
     * @param beanName        Bean 名称
     * @param beanClass       Bean 类型
     * @param constructorArgs 构造函数参数
     */
    public synchronized void registerBean(String beanName, Class<?> beanClass, Object... constructorArgs) {
        if (applicationContext.containsBean(beanName)) {
            log.info("Bean {} 已经存在 开始销毁", beanName);
            destroyBean(beanName);
        }

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
        if (constructorArgs != null) {
            for (Object arg : constructorArgs) {
                beanDefinitionBuilder.addConstructorArgValue(arg);
            }
        }

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        registry.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        log.info("Bean {} 注册成功", beanName);
    }

    /**
     * 动态注册一个已经存在的单例 Bean
     *
     * @param beanName        Bean 名称
     * @param singletonObject 单例 Bean 实例
     */
    public synchronized void registerSingleton(String beanName, Object singletonObject) {
        if (applicationContext.containsBean(beanName)) {
            log.info("Bean {} 已经存在，将被销毁并由新的单例覆盖。", beanName);
            destroyBean(beanName);
        }
        beanFactory.registerSingleton(beanName, singletonObject);
        log.info("Singleton Bean {} 注册成功", beanName);
    }

    /**
     * 销毁指定的 bean，优先移除 BeanDefinition，如果没有则尝试销毁单例实例。
     *
     * @param beanName 要销毁的 bean 名称
     */
    public synchronized void destroyBean(String beanName) {
        // 检查是否为 BeanDefinitionRegistry 以移除 BeanDefinition
        if (beanFactory instanceof BeanDefinitionRegistry registry && registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
            log.info("Bean {} 销毁成功", beanName);
            return;
        }

        // 回退到 DefaultListableBeanFactory 销毁单例
        if (beanFactory instanceof DefaultListableBeanFactory defaultBeanFactory && defaultBeanFactory.containsSingleton(beanName)) {
            defaultBeanFactory.destroySingleton(beanName);
            log.info("Bean {} 销毁成功", beanName);
        }
    }
}