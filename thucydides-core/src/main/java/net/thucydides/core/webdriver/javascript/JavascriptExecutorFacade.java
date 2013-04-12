package net.thucydides.core.webdriver.javascript;

import java.lang.reflect.Field;
import java.util.Map;

import net.thucydides.core.annotations.TypeAdapters;
import net.thucydides.core.webdriver.WebDriverFacade;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static net.thucydides.core.webdriver.javascript.JavascriptSupport.javascriptIsSupportedIn;

/**
 * Simple encapsulation of Javascript execution.
 */
public class JavascriptExecutorFacade {
    private WebDriver driver;
    public GsonBuilder gsonBuilder;

    public JavascriptExecutorFacade(final WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Execute some Javascript in the underlying WebDriver driver.
     * @param script
     * @return
     */
    public Object executeScript(final String script) {
        if (javascriptIsSupportedIn(driver)) {
            JavascriptExecutor js = getJavascriptEnabledDriver();
            return js.executeScript(script);
        } else {
            return null;
        }
    }

    public Object executeScript(final String script, final Object... params) {
        if (javascriptIsSupportedIn(driver)) {
            JavascriptExecutor js = getJavascriptEnabledDriver();
            return js.executeScript(script, params);
        } else {
            return null;
        }
    }
    
    private String getStringifiedJavaScriptObject(final String script, final Object... params){
    	return (String)executeScript("return JSON.stringify(JSON.decycle(function(arguments){"+ script + "}(arguments)));", params);
    }
    
    @SuppressWarnings("unchecked")
	private Gson getGsonForClass(Class<?> clazz){
    	GsonBuilder gsonBuilder = new GsonBuilder();
    	for (Field field : clazz.getDeclaredFields()){
    		if (field.isAnnotationPresent(TypeAdapters.class)){
    			if (!Map.class.isAssignableFrom(field.getType())){
    				throw new WebDriverException("TypeAdapters has to be of Map<Class, Object> type");
    			}
    			field.setAccessible(true);
    			Map<Class<?>, Object> map;
    			try {
					map = (Map<Class<?>, Object>) field.get(clazz.newInstance());
				} catch (IllegalArgumentException e) {
					throw new WebDriverException(e);
				} catch (IllegalAccessException e) {
					throw new WebDriverException(e);
				} catch (InstantiationException e) {
					throw new WebDriverException(e);
				}
    			for (Map.Entry<Class<?>, Object> entry: map.entrySet()){
    				gsonBuilder.registerTypeAdapter(entry.getKey(), entry.getValue());
    			}
    		}
    	}
    	return gsonBuilder.create();
    }
    /**
     * Reflect JavaScript Object on the Java Class.
     * 
     * @param classOfT Java Class to reflect on
     * @param script Script the returns JavaScript Object
     * @param params
     * @return reflected Class
     */
    public <T> T executeScriptAndReflectOn(Class<T> classOfT, final String script, final Object... params){
    	String objString = getStringifiedJavaScriptObject(script, params);
    	gsonBuilder = new GsonBuilder();
    	Gson gson = getGsonForClass(classOfT);
		return gson.fromJson(objString, classOfT);
    }

    private WebDriver getRealDriver() {
        if (WebDriverFacade.class.isAssignableFrom(driver.getClass())) {
            WebDriverFacade driverFacade = (WebDriverFacade) driver;
            return driverFacade.getProxiedDriver();
        } else {
            return driver;
        }
    }

    private JavascriptExecutor getJavascriptEnabledDriver() {
        return (JavascriptExecutor) getRealDriver();
    }

}
