package weservletv2.context;

import weservletv2.beans.config.WeBeanDefinition;
import weservletv2.beans.support.WeBeanDefinitionReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeApplicationContext {
    private String[] configLocations;
    private WeBeanDefinitionReader reader;
    private Map<String,WeBeanDefinition> beanDefinitionMap = new HashMap<>();

    public WeApplicationContext(String... configLocations) {
        this.configLocations = configLocations;

        try {
            //1、加载配置文件
            this.reader = new WeBeanDefinitionReader(this.configLocations);
            List<WeBeanDefinition> beanDefinitions = this.reader.doLoadBeanDefinitions();

            //2、注册到BeanDefinitionMap容器中
            doRegistryBeanDefinitions(beanDefinitions);

            //3、创建IoC容器
            doCreateBean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doCreateBean() {
        for (Map.Entry<String, WeBeanDefinition> beanDefinitionEntry : this.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();
            //创建对象
            //依赖注入
            getBean(beanName);
        }
    }

    private Object getBean(Class className) {
        return this.getBean(className.getName());
    }

    private Object getBean(String beanName) {
        //1、拿到beanName对应的配置信息，即BeanDefinition对象，才能根据配置创建对象
        WeBeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);

        //2、拿到beanDefinition配置信息以后
        return null;
    }

    private void doRegistryBeanDefinitions(List<WeBeanDefinition> beanDefinitions) throws Exception {
        for (WeBeanDefinition beanDefinition : beanDefinitions) {
            if (this.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())){
                throw new Exception("The "+beanDefinition.getFactoryBeanName()+"is exists!!");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
            this.beanDefinitionMap.put(beanDefinition.getBeanClassName(), beanDefinition);
        }
    }
}
