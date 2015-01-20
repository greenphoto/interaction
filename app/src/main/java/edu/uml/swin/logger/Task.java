package edu.uml.swin.logger;

import android.content.Context;

/**
 * Created by jing on 1/19/15.
 */
public abstract class Task {
    private boolean running = false;
    private Context mCtx;

    public Task(Context ctx){
        mCtx = ctx;
    }

    public void start(){
        if(running)
            return;
        onStart();
        running = true;
    }

    public Context getCtx(){
        return mCtx;
    }

    protected abstract void onStart();
    protected abstract void onStop();

    public void stop(){
        if(!running)
            return;
        onStop();
        running = false;
    }
}
