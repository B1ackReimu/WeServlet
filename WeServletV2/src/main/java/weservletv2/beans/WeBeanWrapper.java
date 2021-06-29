package weservletv2.beans;

public class WeBeanWrapper {

    private Object wrapperInstance;
    private Class<?> wrapperClass;

    public WeBeanWrapper(Object instance) {
        this.wrapperInstance = instance;
        this.wrapperClass = this.wrapperInstance.getClass();
    }

    public Object getWrapperInstance() {
        return wrapperInstance;
    }

    public Class<?> getWrapperClass() {
        return wrapperClass;
    }
}
