package dict;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 21.04.2009
 * Time: 16:33:50
 * To change this template use File | Settings | File Templates.
 */
public interface DictionaryInterface {
    public ArrayList<ParonymList> executeQuery(String word, QueryParameters queryParams) throws Exception;

    public ArrayList<ParonymList> findWordNormParonyms(String word, QueryParameters queryParams) throws Exception;

    public ArrayList<ParonymList> findWordFormParonyms(String word, QueryParameters queryParams) throws Exception;


}
