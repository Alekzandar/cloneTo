package finalproject.csci205.com.countdown;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation cd_notification_layout, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under cd_notification_layout.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("finalproject.csci205.com.countcown.cd_notification_layout", appContext.getPackageName());
    }
}
