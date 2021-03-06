package whot.what.hot.ui.login;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import whot.what.hot.R;
import whot.what.hot.base.BaseActivity;
import whot.what.hot.ui.main.MainActivity;
import whot.what.hot.util.CommonUtils;
import whot.what.hot.util.FingerprintAuthenticationDialogFragment;
import whot.what.hot.util.LeetCodePractise;
import whot.what.hot.util.SharedPreferenceUtils;

/**
 * A login screen that offers login via email/password.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class LoginActivity extends BaseActivity implements LoginView {

    // UI references.
    @BindView(R.id.email_login_form)
    LinearLayout emailLoginForm;
    @BindView(R.id.btn_fringerprint)
    Button btnFringerprint;
    @BindView(R.id.et_mail)
    AutoCompleteTextView etMail;
    @BindView(R.id.et_password)
    AutoCompleteTextView etPassword;
    @BindView(R.id.btn_login)
    Button btnLogin;
    @BindView(R.id.login_button)
    LoginButton loginButton;

    private LoginEntity loginEntity;
    private LoginDao loginDao;
    private CallbackManager callbackManager;
    private SharedPreferences mSharedPreferences;
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String KEY_NAME_NOT_INVALIDATED = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        etPassword = findViewById(R.id.et_password);
        etMail.setText(SharedPreferenceUtils.getEmail(this));

        //檢查facebook是否已經登入
        if (CommonUtils.isLoggedIn())
            CommonUtils.intentActivity(LoginActivity.this, MainActivity.class);

        initFingerPrint();
        runLeetCode();
        //讀取sqlite儲存的帳戶資訊
        LoginPresenter presenter = new LoginPresenter(this);
        presenter.onLoadAccount();
    }

    @OnClick(R.id.btn_login)
    @Override
    public void onLoginClick() {

        View focusView;
        // Reset errors.
        etMail.setError(null);
        etPassword.setError(null);

        // Store values at the time of the login attempt.
        final String email = etMail.getText().toString();
        String password = etPassword.getText().toString();
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            etMail.setError(getString(R.string.error_field_required));
            focusView = etMail;
            focusView.requestFocus();
            return;
        }
        // Check for a valid email rule
        if (!CommonUtils.isEmailValid(email)) {
            etMail.setError(getString(R.string.error_invalid_email));
            focusView = etMail;
            focusView.requestFocus();
            return;
        }
        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !CommonUtils.isPasswordValid(password)) {
            etPassword.setError(getString(R.string.error_invalid_password));
            focusView = etPassword;
            focusView.requestFocus();
            return;
        }
        final AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "user.db").build();

        //執行寫入SQLite資料庫的動作
        new Thread() {
            @Override
            public void run() {
                super.run();
                loginDao = db.loginDao();
                loginEntity = new LoginEntity();
                loginEntity.setEmail(email);
                loginDao.insertUsers(loginEntity);
            }
        }.start();

        //紀錄登入的電子郵件到SharedPreference後跳轉頁面到首頁
        SharedPreferenceUtils.setEmail(this, email);
        CommonUtils.intentActivity(this, MainActivity.class);
    }

    @OnEditorAction(R.id.et_password)
    @Override
    public boolean onPasswordEditorDone(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onLoginClick();
            return true;
        }
        return false;
    }

    @OnClick(R.id.login_button)
    @Override
    public void onFacebookLoginClick() {

        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions("email");
        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final AccessToken accessToken = loginResult.getAccessToken();
                GraphRequest.newMeRequest(accessToken, (user, graphResponse) -> CommonUtils.intentActivity(LoginActivity.this, MainActivity.class)).executeAsync();
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
    }

    @Override
    public void fetchAccount(ArrayAdapter<String> adapter) {
        //will start working from first character
        etMail.setThreshold(1);
        //setting the adapter data into the
        etMail.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName                          the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     */
    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Proceed the purchase operation
     *
     * @param withFingerprint {@code true} if the purchase was made by using a fingerprint
     * @param cryptoObject    the Crypto object
     */
    public void onLogin(boolean withFingerprint,
                        @Nullable FingerprintManager.CryptoObject cryptoObject) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            assert cryptoObject != null;
            tryEncrypt(cryptoObject.getCipher());

        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal(SECRET_MESSAGE.getBytes());
            Base64.encodeToString(encrypted, 0 /* flags */);
            CommonUtils.intentActivity(this, MainActivity.class);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            showMessage("Failed to encrypt the data with the generated key. Retry the purchase");
        }
    }

    /**
     * 建立指紋辨識
     */
    private void initFingerPrint() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
        Cipher defaultCipher;
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        assert keyguardManager != null;
        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            btnFringerprint.setEnabled(false);
            return;
        }

        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio
        assert fingerprintManager != null;
        // noinspection ResourceType
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            btnFringerprint.setEnabled(false);
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one" +
                            " fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey(DEFAULT_KEY_NAME, true);
        createKey(KEY_NAME_NOT_INVALIDATED, false);
        btnFringerprint.setEnabled(true);
        btnFringerprint.setOnClickListener(
                new FingerPrintClick(defaultCipher, DEFAULT_KEY_NAME));

    }

    private void runLeetCode() {
//        LeetCodePractise.moveZeroes(new int[]{0,1,0,3,2});
//        LeetCodePractise.reverseString("hello");
//        LeetCodePractise.getSum(1,2);
//        LeetCodePractise.selfDividingNumbers(1,22);
//        LeetCodePractise.islandPerimeter(new int[][]{ {0,1,0,0}, {1,1,1,0}, {0,1,0,0}, {1,1,0,0}});
//        LeetCodePractise.singleNumber(new int[]{2,2,1});
//        LeetCodePractise.detectCapitalUse("USA");
//        LeetCodePractise.canConstruct("aa", "aab");
//        LeetCodePractise.anagramMappings(new int[]{12, 28, 46, 32, 50}, new int[]{50, 12, 32, 46, 28});
//        LeetCodePractise.isToeplitzMatrix(new int[][]{ {11,74,0,93},{40,11,74,7}});
//        LeetCodePractise.findMaxConsecutiveOnes(new int[]{1,0,1,1,0,1});
//        LeetCodePractise.intersection(new int[]{3,1,2},new int[]{1});
//        LeetCodePractise.titleToNumber("AAA");
//        Log.i("TAG", ""+LeetCodePractise.missingNumber(new int[]{9,6,4,2,3,5,7,0,1}));
//        Log.i("TAG", ""+LeetCodePractise.twoSum(new int[]{0,0,3,4},0));
//        Log.i("TAG", ""+LeetCodePractise.firstUniqChar("cc"));
//        Log.i("TAG", ""+LeetCodePractise.majorityElement(new int[]{1}));
//        Log.i("TAG", ""+LeetCodePractise.maximumProduct(new int[]{-4,-3,-2,-1,60}));
//        Log.i("TAG", ""+LeetCodePractise.containsDuplicate(new int[]{1,2,2}));
        Log.i("TAG", "" + LeetCodePractise.toHex(100));
    }

    private class FingerPrintClick implements View.OnClickListener {

        Cipher mCipher;
        String mKeyName;

        FingerPrintClick(Cipher cipher, String keyName) {
            mCipher = cipher;
            mKeyName = keyName;
        }

        @Override
        public void onClick(View view) {
            // Set up the crypto object for later. The object will be authenticated by use
            // of the fingerprint.
            if (CommonUtils.initCipher(mKeyStore, mCipher, mKeyName)) {

                // Show the fingerprint dialog. The user has the option to use the fingerprint with
                // crypto, or you can fall back to using a server-side verified password.
                FingerprintAuthenticationDialogFragment fragment
                        = new FingerprintAuthenticationDialogFragment();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                boolean useFingerprintPreference = mSharedPreferences
                        .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                true);
                if (useFingerprintPreference)
                    fragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                else fragment.setStage(FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                // This happens if the lock screen has been disabled or or a fingerprint got
                // enrolled. Thus show the dialog to authenticate with their password first
                // and ask the user if they want to authenticate with fingerprints in the
                // future
                FingerprintAuthenticationDialogFragment fragment
                        = new FingerprintAuthenticationDialogFragment();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                fragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        }
    }
}

