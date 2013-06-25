package net.thucydides.core.webdriver.javascript;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import net.thucydides.core.pages.jquery.JQueryEnabledPage;
import net.thucydides.core.webdriver.WebDriverFacade;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.reflect.TypeToken;

import static net.thucydides.core.webdriver.javascript.JavascriptSupport.javascriptIsSupportedIn;

/**
 * Simple encapsulation of Javascript execution.
 */
public class JavascriptExecutorFacade {
    private WebDriver driver;
    private ObjectMapper mapper;
    private InjectableValues inject;

    public JavascriptExecutorFacade(final WebDriver driver) {
        this.driver = driver;
    }
    
    public JavascriptExecutorFacade withObjectMapper(ObjectMapper mapper){
    	this.mapper = mapper;
    	return this;
    }
    public JavascriptExecutorFacade withInjectableValues(InjectableValues inject){
    	this.inject = inject;
    	return this;
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
    	JQueryEnabledPage jQueryEnabledPage = JQueryEnabledPage.withDriver(getRealDriver());
        jQueryEnabledPage.injectJavaScriptUtils();
    	return (String)executeScript("return JSON.stringify(JSON.decycle(function(arguments){"+ script + "}(arguments)));", params);
    }
    
    

	private ObjectMapper getMapperForClass(Class<?> clazz){
		if (mapper == null){
			mapper = new ObjectMapper();
		}
    	return mapper;
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
    	ObjectMapper mapper = getMapperForClass(classOfT);
    	ObjectReader reader = mapper.reader(classOfT);
    	if (inject != null){
    		reader = reader.with(inject);
    	}
    	try {
    		return reader.readValue(objString);
		} catch (JsonParseException e) {
			throw new WebDriverException(e);
		} catch (JsonMappingException e) {
			throw new WebDriverException(e);
		} catch (IOException e) {
			throw new WebDriverException(e);
		}
    }
    
    /**
     * Reflect JavaScript Objects on the List of Java Class.
     * 
     * @param classOfT Java Class to reflect on
     * @param script Script the returns JavaScript Object
     * @param params
     * @return reflected List of Class
     */
    public <T> List<T> executeScriptAndReflectOnListOf(Class<T> classOfT, final String script, final Object... params){
    	String objString = getStringifiedJavaScriptObject(script, params);
    	
    	ObjectMapper mapper = getMapperForClass(classOfT);
    	ObjectReader reader = mapper.reader(TypeFactory.defaultInstance().constructCollectionType(List.class, classOfT));
    	if (inject != null){
    		reader = reader.with(inject);
    	}
    	try {
			return reader.readValue(objString);
		} catch (JsonParseException e) {
			throw new WebDriverException(e);
		} catch (JsonMappingException e) {
			throw new WebDriverException(e);
		} catch (IOException e) {
			throw new WebDriverException(e);
		}
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
