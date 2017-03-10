package org.whipper.gui.web;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.whipper.WhipperProperties;

@Singleton
@Startup
public class WhipperJobService{

    private static final char[] ID_CHARS = {
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    public static final int ID_LENGTH = 8;
    private static final int CACHE_LIMIT = 2 << 12;
    private static final String ARTIFACTS_PATH_ABSOLUTE_PROP = "artifacts.path.absolute";
    private static final String SCENARIOS_PATH_ABSOLUTE_PROP = "scenarios.path.absolute";

    private final TreeMap<String, WhipperJob> jobCache = new TreeMap<>();
    private int[] nextId = new int[ID_LENGTH];
    @Inject
    private Context context;

    @Lock(LockType.WRITE)
    public String startNewJob(WhipperProperties props){
        String id = nextFreeId();
        props.setOutputDir(context.getResultDir(id).getAbsolutePath());
        if(!props.getProperty(ARTIFACTS_PATH_ABSOLUTE_PROP, boolean.class, false)){
            props.setArtifacstDir(context.getArtifactDir(props.getArtifacstDirStr()).getAbsolutePath());
        }
        if(!props.getProperty(SCENARIOS_PATH_ABSOLUTE_PROP, boolean.class, false)){
            props.setScenario(context.getScenarioDir(props.getScenarioStr()).getAbsolutePath());
        }
        WhipperJob wj = new WhipperJob(id, props);
        storeJob(id, wj);
        wj.start();
        return id;
    }

    @Lock(LockType.READ)
    public WhipperJob getJob(String id){
        WhipperJob wj = jobCache.get(id);
        if(wj == null){
            wj = readJob(id);
        }
        return wj;
    }

    @Lock(LockType.WRITE)
    public void deleteJob(String id){
        if(id == null){
            return;
        }
        WhipperJob wj = jobCache.remove(id);
        if(wj != null){
            Utils.delete(wj.getFullPathToJob());
        }
    }

    private WhipperJob readJob(String key){
        
        
        
        
        
        
        return null;
        // TODO - read job; null if not found
    }

    private void storeJob(String key, WhipperJob job){
        if((jobCache.size() >= CACHE_LIMIT) && !jobCache.containsKey(key)){
            // clear old jobs
            Set<String> keys = jobCache.navigableKeySet();
            int i = 0;
            int size = keys.size();
            for(Iterator<String> iter = keys.iterator(); iter.hasNext() && i < size; i++){
                iter.next();
                iter.remove();
            }
        }
        jobCache.put(key, job);
    }

    private String nextFreeId(){
        String id;
        do{
            id = nextId();
        } while(context.getResultDir(id).exists());
        return id;
    }

    private String nextId(){
        char[] idChrs = new char[ID_LENGTH];
        for(int i = 0; i < ID_LENGTH; i++){
            idChrs[i] = ID_CHARS[nextId[i]];
        }

        boolean next;
        int i = 0;
        do{
            if(i == ID_LENGTH){
                Utils.LOG.warn("Job ID counter overflow. Next ID starts from beginning.");
                Arrays.fill(nextId, 0);
                next = false;
            } else {
                nextId[i]++;
                if(nextId[i] == ID_CHARS.length){
                    nextId[i] = 0;
                    i++;
                    next = true;
                } else {
                    next = false;
                }
            }
        } while (next);

        return new String(idChrs);
    }
}
