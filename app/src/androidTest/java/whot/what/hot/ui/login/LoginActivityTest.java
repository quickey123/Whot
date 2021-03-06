package whot.what.hot.ui.login;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import whot.what.hot.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/** 登入畫面的 UI test，測試所有使用行為
 * Created by Kevin on 19/10/2017.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {
    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(
            LoginActivity.class);

    @Test
    public void testLoginClick() throws Exception {
        // Type text and then press the button.
//        onView(withId(R.id.et_mail))
//                .perform(typeText("123"), closeSoftKeyboard());
//        onView(withId(R.id.et_mail))
//                .check(matches(withText("123")));


//        onView(withId(R.id.et_password))
//                .perform(typeText("123456"), closeSoftKeyboard());
//        onView(withId(R.id.et_password))
//                .check(matches(withText("123456")));

        Thread.sleep(2000);
        //測試登入按鈕
        onView(withId(R.id.btn_login))
                .perform(click());

    }

    @Test
    public void onPasswordEditorDone() throws Exception {

    }
}
