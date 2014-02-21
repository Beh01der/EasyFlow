package au.com.ds.ef;

/**
 * User: andrey
 * Date: 20/02/2014
 * Time: 10:41 PM
 */
public interface FlowLogger {
    void info(String message, Object... o);
    void error(String message, Throwable e);
}
