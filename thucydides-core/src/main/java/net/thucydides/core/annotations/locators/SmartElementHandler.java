package net.thucydides.core.annotations.locators;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.thucydides.core.annotations.DelayElementLocation;
import net.thucydides.core.annotations.implementedBy;
import net.thucydides.core.pages.WebElementFacade;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;


public class SmartElementHandler implements InvocationHandler{	
    private final ElementLocator locator;
    private final WebDriver driver;
    private final Class<? extends WebElementFacade> implementerClass;
    private final long timeoutInMilliseconds;
    
    private Class<? extends WebElementFacade> getImplementer(Class<? extends WebElementFacade> interfaceType){
    	implementedBy implBy = interfaceType.getAnnotation(implementedBy.class);
    	Class<? extends WebElementFacade> implementerClass = implBy.value();
    	if (!interfaceType.isAssignableFrom(implementerClass)) {
    		throw new RuntimeException("implementer Class does not implement the interface " + interfaceType.getName());
    	}
    	return implementerClass;
    }

    public SmartElementHandler(Class<? extends WebElementFacade> interfaceType, ElementLocator locator,
			WebDriver driver, long timeoutInMilliseconds) {
    	this.driver = driver;
        this.locator = locator;
        if (!WebElementFacade.class.isAssignableFrom(interfaceType)) {
            throw new RuntimeException("interface not assignable to WebElementFacade");
        }
        
        this.implementerClass = getImplementer(interfaceType);
        this.timeoutInMilliseconds = timeoutInMilliseconds; 
    }

	public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
    	try {
    		Constructor<? extends WebElementFacade> constructor = null;
	    	if (method.isAnnotationPresent(DelayElementLocation.class) || method.getName().equals("toString")) {
	    		//first try to get the proper constructor from the implementer class
	    		try{
	    			constructor = implementerClass.getConstructor(WebDriver.class, ElementLocator.class, long.class);
	    		} catch (NoSuchMethodException e){
	    		/*if failed look for the first available constructor with ElementLocator in the parent classes
	    		 * Eventually WebElementFacadeImpl will be reached, if no other parents implement this constructor 
	    		 */
	    			Constructor<? extends WebElementFacade> tempConstructor = implementerClass.getConstructor(WebDriver.class,WebElement.class, long.class);
	    			constructor = tempConstructor.newInstance((WebDriver)null, (WebElement)null, 0).getConstructorWithLocator(); 
	    		}
	    		Object webElementFacadeExt = constructor.newInstance(driver, locator, timeoutInMilliseconds);
	            return method.invoke(webElementFacadeExt, objects);
	            
	        }
	        WebElement element = locator.findElement();
	
	        if ("getWrappedElement".equals(method.getName())) {
	            return element;
	        }
	        
	        constructor = implementerClass.getConstructor(WebDriver.class,WebElement.class, long.class);
	        Object webElementFacadeExt = constructor.newInstance(driver, element, timeoutInMilliseconds);
	        //try {
	            return method.invoke(implementerClass.cast(webElementFacadeExt), objects);
        } catch (InvocationTargetException e) {
            // Unwrap the underlying exception
            throw e.getCause();
        }
    }
	
}

