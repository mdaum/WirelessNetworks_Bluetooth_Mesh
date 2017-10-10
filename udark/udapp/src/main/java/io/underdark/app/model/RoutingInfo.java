package io.underdark.app.model;

/**
 * Created by mdaum on 10/10/2017.
 */

public class RoutingInfo {
    private long routerDest;
    private int step;
    public RoutingInfo(long routerDest, int step){
        this.step = step;
        this.routerDest=routerDest;
    }
    public long getRouterDest(){
        return routerDest;
    }
    public int getStep(){
        return step;
    }
    public void setRouterDest(long val){
        routerDest=val;
    }
    public void setStep(int val){
        step=val;
    }
}
