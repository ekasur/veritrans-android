package id.co.veritrans.sdk.uiflow.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import id.co.veritrans.sdk.coreflow.callback.CheckoutCallback;
import id.co.veritrans.sdk.coreflow.callback.PaymentOptionCallback;
import id.co.veritrans.sdk.coreflow.core.Constants;
import id.co.veritrans.sdk.coreflow.core.LocalDataHandler;
import id.co.veritrans.sdk.coreflow.core.Logger;
import id.co.veritrans.sdk.coreflow.core.TransactionRequest;
import id.co.veritrans.sdk.coreflow.core.VeritransSDK;
import id.co.veritrans.sdk.coreflow.models.CustomerDetails;
import id.co.veritrans.sdk.coreflow.models.PaymentMethodsModel;
import id.co.veritrans.sdk.coreflow.models.TransactionResponse;
import id.co.veritrans.sdk.coreflow.models.UserDetail;
import id.co.veritrans.sdk.coreflow.models.snap.Token;
import id.co.veritrans.sdk.coreflow.models.snap.Transaction;
import id.co.veritrans.sdk.coreflow.models.snap.TransactionResult;
import id.co.veritrans.sdk.coreflow.utilities.Utils;
import id.co.veritrans.sdk.uiflow.PaymentMethods;
import id.co.veritrans.sdk.uiflow.R;
import id.co.veritrans.sdk.uiflow.adapters.PaymentMethodsAdapter;
import id.co.veritrans.sdk.uiflow.utilities.SdkUIFlowUtil;

/**
 * Displays list of available payment methods.
 * <p/>
 * Created by shivam on 10/16/15.
 */
public class PaymentMethodsActivity extends BaseActivity{

    public static final String PAYABLE_AMOUNT = "Payable Amount";
    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.3f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.7f;
    private static final float ALPHA = 0.6f;
    private static final String TAG = PaymentMethodsActivity.class.getSimpleName();
    private static final float PERCENTAGE_TOTAL = 1f;
    private ArrayList<PaymentMethodsModel> data = new ArrayList<>();

    private VeritransSDK veritransSDK = null;
    private boolean isHideToolbarView = false;

    //Views
    private Toolbar toolbar = null;
    private AppBarLayout mAppBarLayout = null;
    private RecyclerView mRecyclerView = null;
    private TextView headerTextView = null;
    private TextView textViewMeasureHeight = null;
    private TextView amountText = null;
    private LinearLayout progressContainer = null;
    private ImageView logo = null;
    private ArrayList<String> bankTrasfers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments_method);
        veritransSDK = VeritransSDK.getVeritransSDK();
        initializeTheme();

        UserDetail userDetail = null;
        try {
            userDetail = LocalDataHandler.readObject(getString(R.string.user_details), UserDetail.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        TransactionRequest transactionRequest = null;
        if (veritransSDK != null) {
            transactionRequest = veritransSDK.getTransactionRequest();
            if(transactionRequest != null){
                CustomerDetails customerDetails = null;
                if (userDetail != null) {
                    customerDetails = new CustomerDetails(userDetail.getUserFullName(), null,
                            userDetail.getEmail(), userDetail.getPhoneNumber());
                    transactionRequest.setCustomerDetails(customerDetails);
                    Logger.d(String.format("Customer name: %s, Customer email: %s, Customer phone: %s", userDetail.getUserFullName(), userDetail.getEmail(), userDetail.getPhoneNumber() ));
                }
                setUpPaymentMethods();
            }else{
                showErrorAlertDialog(getString(R.string.error_transaction_empty));
            }
        } else {
            Logger.e("Veritrans SDK is not started.");
            finish();
        }
    }

    /**
     * if recycler view fits within screen then it will disable the scrolling of it.
     */
    private void handleScrollingOfRecyclerView() {
        headerTextView.setAlpha(1);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView
                        .getLayoutManager();

                int hiddenTextViewHeight = textViewMeasureHeight.getHeight();
                int recyclerViewHieght = mRecyclerView.getMeasuredHeight();
                int appBarHeight = mAppBarLayout.getHeight();
                int exactHeightForCompare = hiddenTextViewHeight - appBarHeight;

                int oneRowHeight = dp2px(60);
                int totalHeight = (mRecyclerView.getAdapter().getItemCount() - 1) * oneRowHeight;

                if (totalHeight < exactHeightForCompare) {
                    disableScrolling();
                    headerTextView.setAlpha(1);
                }
            }
        }, 200);
    }

    /**
     * Disable scrolling of recycler view.
     */
    private void disableScrolling() {
        //turn off scrolling
        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams)
                mAppBarLayout.getLayoutParams();
        appBarLayoutParams.setBehavior(null);
        mAppBarLayout.setLayoutParams(appBarLayoutParams);
    }

    /**
     * bind views , initializes adapter and set it to recycler view.
     */
    private void setUpPaymentMethods() {

        //initialize views
        bindActivity();

        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bindDataToView();
        getPaymentPages();
    }

    private void setupRecyclerView() {
        PaymentMethodsAdapter paymentMethodsAdapter = new PaymentMethodsAdapter(this, data);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(paymentMethodsAdapter);

        // disable scrolling of recycler view if there is no need of it.
        handleScrollingOfRecyclerView();
    }

    /**
     * set data to view.
     */
    private void bindDataToView() {

        VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            String amount = getString(R.string.prefix_money, Utils.getFormattedAmount(veritransSDK.getTransactionRequest().getAmount()));

            amountText.setText(amount);
        }

    }

    /**
     * initialize views.
     */
    private void bindActivity() {
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_payment_methods);
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.main_appbar);
        headerTextView = (TextView) findViewById(R.id.title_header);
        amountText = (TextView) findViewById(R.id.header_view_sub_title);
        textViewMeasureHeight = (TextView) findViewById(R.id.textview_to_compare);
        logo = (ImageView) findViewById(R.id.merchant_logo);
        progressContainer = (LinearLayout) findViewById(R.id.progress_container);
    }

    private void getPaymentPages() {
        progressContainer.setVisibility(View.VISIBLE);
        veritransSDK.checkout(new CheckoutCallback() {
            @Override
            public void onSuccess(Token token) {
                LocalDataHandler.saveString(Constants.AUTH_TOKEN, token.getTokenId());
                getPaymentOptions(token.getTokenId());
            }

            @Override
            public void onFailure(Token token, String reason) {
                showErrorMessage();
            }

            @Override
            public void onError(Throwable error) {
                showErrorMessage();
            }
        });
    }

    private void getPaymentOptions(String tokenId) {
        veritransSDK.getPaymentOption(tokenId, new PaymentOptionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {
                try{
                    progressContainer.setVisibility(View.GONE);
                    String logoUrl = transaction.getMerchantData().getLogoUrl();
                    String merchantName = transaction.getMerchantData().getDisplayName();
                    veritransSDK.setMerchantLogo(logoUrl);
                    veritransSDK.setMerchantName(merchantName);
                    showLogo(logoUrl);
                    for (String bank : transaction.getTransactionData().getBankTransfer().getBanks()) {
                        bankTrasfers.add(bank);
                    }
                    List<String> paymentMethods = transaction.getTransactionData().getEnabledPayments();
                    initialiseAdapterData(paymentMethods);
                    setupRecyclerView();
                } catch (NullPointerException e){
                    Logger.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onFailure(Transaction transaction, String reason) {
                progressContainer.setVisibility(View.GONE);
                showDefaultPaymentMethods();
            }

            @Override
            public void onError(Throwable error) {
                progressContainer.setVisibility(View.GONE);
                showDefaultPaymentMethods();
            }
        });
    }

    /**
     * initialize adapter data model by dummy values.
     */
    private void initialiseAdapterData(List<String> enabledPayments) {
        data.clear();
        for (String paymentType : enabledPayments) {
            PaymentMethodsModel model = PaymentMethods.getMethods(this, paymentType);
            if (model != null) {
                data.add(model);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            SdkUIFlowUtil.hideKeyboard(this);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * sends broadcast for transaction details.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Logger.d(TAG, "in onActivity result : request code is " + requestCode + "," + resultCode);

        if (requestCode == Constants.RESULT_CODE_PAYMENT_TRANSFER) {
            Logger.d(TAG, "sending result back with code " + requestCode);

            if (resultCode == RESULT_OK) {
                TransactionResponse response = (TransactionResponse) data.getSerializableExtra(getString(R.string.transaction_response));
                if (response != null) {
                    if (response.getStatusCode().equals(getString(R.string.success_code_200))) {
                        veritransSDK.notifyTransactionFinished(new TransactionResult(response, null, TransactionResult.STATUS_SUCCESS));
                    } else if (response.getStatusCode().equals(getString(R.string.success_code_201))) {
                        veritransSDK.notifyTransactionFinished(new TransactionResult(response, null, TransactionResult.STATUS_PENDING));
                    } else {
                        veritransSDK.notifyTransactionFinished(new TransactionResult(response, null, TransactionResult.STATUS_FAILED));
                    }
                } else {
                    veritransSDK.notifyTransactionFinished(new TransactionResult());
                }
                finish();
            }

        } else {
            Logger.d(TAG, "failed to send result back " + requestCode);
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void showLogo(String url) {
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(this)
                    .load(url)
                    .into(logo);
        } else {
            logo.setVisibility(View.INVISIBLE);
        }
    }

    private void showDefaultPaymentMethods() {
        progressContainer.setVisibility(View.GONE);
        List<String> paymentMethods = PaymentMethods.getDefaultPaymentList(this);
        initialiseAdapterData(paymentMethods);
        setupRecyclerView();
    }

    private void showErrorMessage() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.txt_error_snap_token))
                .setPositiveButton(R.string.btn_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        getPaymentPages();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .create();
        alert.show();
    }

    private void showErrorAlertDialog(String message){
        AlertDialog alert = new AlertDialog.Builder(this)
                .setMessage(message)
                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .create();
        alert.show();
    }

    public ArrayList<String> getBankTrasfers() {
        return bankTrasfers;
    }
}