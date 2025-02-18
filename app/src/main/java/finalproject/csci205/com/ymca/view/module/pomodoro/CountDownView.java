package finalproject.csci205.com.ymca.view.module.pomodoro;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import finalproject.csci205.com.countdown.Service.CountDownIntent;
import finalproject.csci205.com.countdown.Service.CountDownListener;
import finalproject.csci205.com.countdown.Service.CountDownService;
import finalproject.csci205.com.countdown.Ult.ServiceState;
import finalproject.csci205.com.ymca.R;
import finalproject.csci205.com.ymca.model.PomodoroSettings;
import finalproject.csci205.com.ymca.presenter.PomodoroPresenter;
import finalproject.csci205.com.ymca.util.NotificationUtil;

/**
 * Homebrew CountDownView that defines a generic count down timer. Uses custom built
 * count down service to keep track of ticking time. Deploys a notification that is inSync with
 * said view!
 *
 * @author Charles
 */
public class CountDownView extends LinearLayout implements
        View.OnClickListener,
        ServiceConnection,
        CountDownListener,
        NotificationClickedSyncListener {

    /**
     * Static refrence of this, used with BroadCastReceiver
     */
    private static CountDownView cdView;
    /**
     * Pending intent general paramter
     */
    final int pendingParam = 0;
    /**
     * Service constant, 0 is the rebind code.
     */
    private final int REBINDSERVICE = 0;
    /**
     * Notificaion ID
     */
    private final int TIMERUP_ID = 9;
    /**
     * Internal click counter
     */
    private int startPauseCounter = 0;
    /**
     * View elements
     */
    private View root;
    private TextView mins;
    private TextView seconds;
    private ImageButton cancelPom;
    private LinearLayout timerContainer;
    /**
     * Service refrence
     */
    private CountDownService cd;
    /**
     * Notificaion view, system manangers, and class refrence for view returning
     */
    private RemoteViews notificationView;
    private NotificationManager notificationManager;
    private Notification notification;
    private Class jumpTo;
    /**
     * Pomodoro constants
     */
    private int sessionTime;
    private int breakTime;
    private int numCyclesTillBreak;
    private int longBreakTime;
    /**
     * Determines if system should countdown as a break, or work peroid.
     * On first run, it should be true.
     */
    private boolean breakMode = true;
    /**
     * Date formating
     */
    private Date date = null;
    private DateFormat minFor;
    /**
     * Presenter and Model refrences.
     * NOTE, the settings variable exists for convience, it still commes from the presenter!
     */
    private PomodoroPresenter presenter;
    private PomodoroSettings settings;

    public CountDownView(Context context) {
        super(context);
        init();
    }

    public CountDownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Creates view elements
     *
     * @author Charles
     */
    private void init() {
        root = inflate(getContext(), R.layout.countdown_layout, this);
        mins = (TextView) root.findViewById(R.id.mins);
        seconds = (TextView) root.findViewById(R.id.seconds);
        cancelPom = (ImageButton) root.findViewById(R.id.cancelBtn);
        cancelPom.setVisibility(GONE);
        timerContainer = (LinearLayout) root.findViewById(R.id.layoutContainer);
        timerContainer.setOnClickListener(this);
        cancelPom.setOnClickListener(this);
        cdView = this;
        minFor = new SimpleDateFormat("mm");
        pomodoroDataUpdate();

        /**
         * Service logic
         */
        CountDownIntent i = new CountDownIntent(getContext(), sessionTime);
        if (isMyServiceRunning(CountDownService.class)) {
            getContext().bindService(i, this, REBINDSERVICE);

        } else {
            getContext().bindService(i, this, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Creates Pomodoro Data Objects when user chooses to save new variables.
     *
     * @author Charles
     */
    private void pomodoroDataUpdate() {
        //Pomodoro data
        presenter = new PomodoroPresenter();
        settings = presenter.getSavedPomSettings();
        if (settings != null) {
            this.sessionTime = settings.getSessionTime();
            setInternalSettings();
        } else {
            this.sessionTime = 0;
        }
    }

    /**
     * Inits local vars with stored settings if they exist
     *
     * @author Charles
     */
    public void setInternalSettings() {
        //Pomodoro Init;

        if (settings != null) {
            breakTime = settings.getNormBreakTime();
            numCyclesTillBreak = settings.getNumCyclesTillBreak();
            longBreakTime = settings.getLongBreak();
        }
    }

    /**
     * Used saved new data to PomodoroSettings, we must update the view now
     *
     * @author Charles
     */
    public void newSavedConfig() {
        pomodoroDataUpdate();
        setInternalSettings();
        sessionTime = settings.getSessionTime();
        breakMode = true;
    }

    /**
     * Updates view and notification to reflect accurate process
     *
     * @param l -  time
     * @author Charles
     */
    private void updateProgress(long l) {
        Date date = new Date(l);
        DateFormat minFor = new SimpleDateFormat("mm");
        DateFormat secFor = new SimpleDateFormat("ss");
        mins.setText(minFor.format(date));
        seconds.setText(secFor.format(date));

        notificationView.setTextViewText(R.id.ticker, minFor.format(date) + " : " + secFor.format(date));
        notificationManager.notify(NotificationUtil.NOTIFICATION_ID, notification);
    }


    /**
     * Note, implementing onClick in this view takes presidence over any other View that
     * creates this object. It is INTENDED to be a create and forget utility
     *
     * @param view
     * @author Charles
     */
    @Override
    public void onClick(View view) {

        if (cd != null) {
            if (view.getId() == timerContainer.getId()) {
                resumeOrPause();
            } else if (view.getId() == cancelPom.getId()) {
                Log.i("click", "CANCEL/END");
                cancelPom.setVisibility(GONE);
                countCancelComplete();
            }
        }
    }

    /**
     * Resumes or pauses service depending on num of clicks
     *
     * @author Charles
     */
    private void resumeOrPause() {
        if (sessionTime != 0) { //Check to see if they configured the timer!
            if (startPauseCounter == 0) { //Start
                cancelPom.setVisibility(VISIBLE);
                cd.startTimer();
            } else if (startPauseCounter % 2 != 0) { //Click to pause
                cd.pauseTimer();
            } else { //Click to resume
                cd.resume();
            }
            startPauseCounter++;
        } else {
            Toast.makeText(getContext(), "Please configure the Pomodoro timer first!", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Handles view if the counter was manually canceled by the user, or ran about its
     * time on its own. This method handles the functionality that is shared between both
     * events. Method will also configure the timer for breaks@|!
     *
     * @author Charles
     */
    private void countCancelComplete() {
        //Formatting variables, doing general reset
        cancelPom.setVisibility(INVISIBLE);
        NotificationUtil.destroyPomNotification(getContext());
        numCyclesTillBreak--; //Decrement counter
        seconds.setText("00");
        startPauseCounter = 0;
        cd.stopTimer();
        /**
         * If breakMode == false, then config completion for another work peroid
         * If true, then config for a break.
         */
        if (!breakMode) {
            sessionTime = settings.getSessionTime();
            configBreakOrPomoSession(sessionTime);
            breakMode = true;
        } else {

            //Pomodoro internals
            if (numCyclesTillBreak < 0) { //Time for a long break
                Toast.makeText(getContext(), "Long Break time!", Toast.LENGTH_SHORT).show();
                sessionTime = longBreakTime;
                configBreakOrPomoSession(sessionTime);
                setInternalSettings();

            } else { //Time for a short break
                Toast.makeText(getContext(), "Short Break time!", Toast.LENGTH_SHORT).show();
                sessionTime = breakTime;
                configBreakOrPomoSession(sessionTime);

            }
            breakMode = false;

        }
        //Reset Service Internals.
        if (isMyServiceRunning(CountDownService.class)) {
            cd.resetStoredTime();
            NotificationUtil.destroyPomNotification(getContext());
            getContext().unbindService(this);
        }
    }

    /**
     * Helper method that resets internal values depending on if we have a break or not.
     *
     * @param newTimeValue
     * @author Charles
     */
    private void configBreakOrPomoSession(int newTimeValue) {
        final int conversionMiliConstant = 60000;
        cd.setSessionTime(newTimeValue);
        date = new Date((long) newTimeValue * conversionMiliConstant);
        mins.setText(minFor.format(date));
    }

    /**
     * Sets CD_Service session time externally
     *
     * @author Charles
     */
    public void setCDTime(int time) {
        cd.setSessionTime(time);
    }



    /**
     * Gets the binder upon successfull connection, creates our local refrence to our service
     * Specifc to the view, sets the counter to a proper value depending on state.
     * Sets Callback listener for value updates.
     *
     * @param componentName
     * @param iBinder
     * @author Charles
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        CountDownService.CountDownBinder binder = (CountDownService.CountDownBinder) iBinder;
        cd = binder.getService();
        cd.setCountDownListener(this);
        if (cd.getState() == ServiceState.ISRUNNING) {
            startPauseCounter = 1;
        } else if (cd.getState() == ServiceState.PAUSED) {
            startPauseCounter = 2;
        }
        configState();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    private void configState() {
        updateProgress(cd.getStoredTime());
    }

    /**
     * Checks to see if service passed is running
     * @see http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Call Back from service.
     *
     * @param l
     * @author Charles
     */
    @Override
    public void countdownResult(long l) {
        updateProgress(l);
    }

    /**
     * Call back from Service notifying that the count down is complete
     * Reset View.
     * @author Charles
     */

    /**
     * Handles countdown completion
     *
     * @author Charles
     */
    @Override
    public void onCountFinished() {
        final int twoSeconds = 2000; //Parameter takes miliseconds.
        countCancelComplete();
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(twoSeconds);

        //the intent that is started when the notification is clicked (works)
        Intent notificationIntent = new Intent(getContext(), jumpTo);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final int countFinishedid = 5; //Need not be global, self dismissing.
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(getContext(), countFinishedid,
                notificationIntent, pendingParam);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getContext())
                        .setSmallIcon(R.drawable.ic_pom)
                        .setContentTitle(getResources().getString(R.string.notification_title))
                        .setContentText(getResources().getString(R.string.notification_context))
                        .setAutoCancel(true)
                        .setContentIntent(pendingNotificationIntent);
        notificationManager.notify(TIMERUP_ID, mBuilder.build());
    }


    /**
     * Series of Handles that keeps view and notification actions in sync
     *
     * @author
     */
    @Override
    public void onStartClicked() {
        resumeOrPause();
    }

    @Override
    public void onPausedClicked() {
        resumeOrPause();
    }

    @Override
    public void onStopClicked() {
        countCancelComplete();
    }

    /**
     * Creates notification that pairs the same action logic tied with the CountDownView
     *
     * @author Charles
     */
    private void startNotification() {
        notificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notification = new Notification(R.drawable.ic_pom, null,
                NotificationUtil.NOTIFICATION_ID_CONSTANT);


        notificationView = new RemoteViews(getContext().getPackageName(),
                R.layout.cd_notification_layout);

        //the intent that is started when the notification is clicked (works)
        Intent notificationIntent = new Intent(getContext(), jumpTo);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final int pendingID = 4; //Need not be global, self managing.
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(getContext(), pendingID,
                notificationIntent, pendingParam);

        notification.contentView = notificationView;
        notification.contentIntent = pendingNotificationIntent;
        notification.flags |= Notification.FLAG_NO_CLEAR;


        /* Create intent, set context and destionation */
        Intent start = new Intent(getContext(), NotificationButtonListener.class);
        /* Put extra to identify which action was called. */
        start.putExtra(NotificationUtil.START, NotificationUtil.START);
        final int startId = 0; //Need not be global, self managing.
        PendingIntent pendingStart = PendingIntent.getBroadcast(getContext(), startId,
                start, pendingParam);

        /* Create intent, set context and destionation */
        Intent pauseIntent = new Intent(getContext(), NotificationButtonListener.class);
        /* Put extra to identify which action was called. */
        pauseIntent.putExtra(NotificationUtil.PAUSE, NotificationUtil.PAUSE);
        final int pauseID = 1; //Need not be global, self managing.
        PendingIntent pendingPause = PendingIntent.getBroadcast(getContext(), pauseID,
                pauseIntent, pendingParam);


        /* Create intent, set context and destionation */
        Intent cancelIntent = new Intent(getContext(), NotificationButtonListener.class);
        /* Put extra to identify which action was called. */
        cancelIntent.putExtra(NotificationUtil.CANCEL, NotificationUtil.CANCEL);
        final int cancelID = 2; //Need not be global, self managing.
        PendingIntent pendingCancel = PendingIntent.getBroadcast(getContext(), cancelID,
                cancelIntent, pendingParam);

        /* Functionally equal to setOnClickListener */
        notificationView.setOnClickPendingIntent(R.id.start, pendingStart);
        notificationView.setOnClickPendingIntent(R.id.pause, pendingPause);
        notificationView.setOnClickPendingIntent(R.id.cancel, pendingCancel);
        notificationManager.notify(NotificationUtil.NOTIFICATION_ID, notification);

    }

    /**
     * Defines which class the notification is to open when it is clicked
     * Once that is complete, the notification is read to be deployed.
     *
     * @param jumpTo
     * @author Charles
     */
    public void setJumpTo(Class jumpTo) {
        this.jumpTo = jumpTo;
        startNotification();
    }

    public CountDownService getCd() {
        return cd;
    }


    /**
     * Broadcast Receiver that intercepts Notification Button Clicks
     *
     * @author Charles
     */
    public static class NotificationButtonListener extends BroadcastReceiver {

        /**
         * Determines which button was clicked, then calls the correct listener on
         * CountDownView to interpert the action. This ensures that both the View
         * and notification are synced.
         *
         * @param context
         * @param intent
         * @author Charles
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationClickedSyncListener clickedSyncListener = cdView;
            String startPotential = intent.getStringExtra(NotificationUtil.START);
            String pausePotential = intent.getStringExtra(NotificationUtil.PAUSE);
            String cancelPotential = intent.getStringExtra(NotificationUtil.CANCEL);
            if (startPotential != null) {
                clickedSyncListener.onStartClicked();
            } else if (pausePotential != null) {
                clickedSyncListener.onPausedClicked();
            } else if (cancelPotential != null) {
                clickedSyncListener.onStopClicked();
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NotificationUtil.NOTIFICATION_ID);
            }
        }
    }

}
