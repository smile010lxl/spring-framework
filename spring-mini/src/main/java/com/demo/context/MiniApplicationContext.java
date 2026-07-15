package com.demo.context;

import com.demo.MiniJdkDynamicAopProxy;
import com.demo.service.AService;
import com.demo.service.BService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class MiniApplicationContext {

	private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<String, BeanDefinition>();

	// 单例池，一级缓存
	private Map<String, Object> singletonObjects = new HashMap<String, Object>();
	// 单例池，早期临时对象，二级缓存
	private Map<String, Object> earlyObjects = new HashMap<String, Object>();
	// 单例池，三级缓存
	private Map<String, ObjectFactory> factoriesObjects = new HashMap<String, ObjectFactory>();

	public MiniApplicationContext(Class<?> clazz) {
		refresh();
	}

	private void refresh() {
		loadBeanDefinition();
	}

	private void loadBeanDefinition() {
		RootBeanDefinition aBeanDefinition = new RootBeanDefinition(AService.class);
		RootBeanDefinition bBeanDefinition = new RootBeanDefinition(BService.class);
		beanDefinitionMap.put("aService", aBeanDefinition);
		beanDefinitionMap.put("bService", bBeanDefinition);
	}

	private void finishBeanFactoryInitialization() {
		beanDefinitionMap.keySet().forEach(beanName -> {
			BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
			if (!beanDefinition.isLazyInit() && !beanDefinition.isPrototype()) {
				// createBean
			}
		});
	}

	public Object getBean(String beanName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		// 双重检查锁，检查第一遍
		Object bean = getSingleton(beanName);
		if (bean != null) {
			return bean;
		}
		// 双重检查锁，检查第二遍
		synchronized (singletonObjects) {
			bean = getSingleton(beanName);
			if (bean != null) {
				return bean;
			}
		}

		// 真正开始创建bean  早期bean对象，还没有依赖注入
		RootBeanDefinition beanDefinition = (RootBeanDefinition) beanDefinitionMap.get(beanName);
		Class<?> beanClass = beanDefinition.getBeanClass();
		Object beanInstance = beanClass.getConstructor().newInstance();
		// 创建AOP代理对象 ，有循环依赖的情况才在这里创建代理对象
		//判断当前依赖是循环依赖才创建，怎么判断呢？

		factoriesObjects.put(beanName, new ObjectFactory() {
			@Override
			public Object getObject() throws BeansException {
				return new MiniJdkDynamicAopProxy(beanInstance).getProxy();
			}
		});
		// earlyObjects是多线程的竞争资源
//		earlyObjects.put(beanName, beanInstance);
		//earlyObjects.put(beanName, aopBeanInstance);
		// 依赖注入  byType byName byConstructor default(@Autowared) ByName
		for (Field declaredField : beanClass.getDeclaredFields()) {
			if (declaredField.isAnnotationPresent(Autowired.class)) {
				String fieldName = declaredField.getName();
				Object fieldValue = getBean(fieldName);
				declaredField.setAccessible(true);// 这是private的，需要打开访问权限
				declaredField.set(beanInstance, fieldValue);
			}
		}
		// 初始化
		//if(beanInstance instanceof **){}
		// 正常的还是在初始化之后创建代理对象
		singletonObjects.put(beanName, beanInstance);
		earlyObjects.remove(beanName);
		return bean;
	}

	private Object getSingleton(String beanName) {
		if (singletonObjects.containsKey(beanName)) {
			return singletonObjects.get(beanName);
		}
		//充当循环依赖的入口
		synchronized (singletonObjects) {
			// 出口 二级缓存获取
			// 在这里可以判断是否有循环依赖，只要二级缓存有，就说明这个对象有循环依赖
			if (earlyObjects.containsKey(beanName)) {
				return earlyObjects.get(beanName);
			}
			if(factoriesObjects.containsKey(beanName)){
				Object aopObject= factoriesObjects.get(beanName).getObject();
				// 将代理对象存入二级缓存，为了防止重复创建aop动态代理
				earlyObjects.put(beanName, aopObject);
				return aopObject;
			}
		}
		return null;
	}
}
