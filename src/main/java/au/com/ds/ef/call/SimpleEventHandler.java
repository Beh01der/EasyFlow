package au.com.ds.ef.call;

import au.com.ds.ef.*;

/**
 * User: andrey
 * Date: 6/12/2013
 * Time: 11:35 AM
 */
public interface SimpleEventHandler<C extends StatefulContext> extends Handler {
    void call(EventEnum event, C context) throws Exception;
}