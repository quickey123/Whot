package kevin.practise.example.ui.main;

import android.util.Log;

import java.util.HashMap;

import kevin.practise.example.base.BasePresenter;
import kevin.practise.example.data.MainModel;
import kevin.practise.example.data.WeatherDataBean;
import kevin.practise.example.http.RxManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 處理 main 業務邏輯
 * Created by kevin on 21/09/2017.
 */

class MainPresenter extends BasePresenter<MainView> {

    MainPresenter(MainView view) {
        attachView(view);
    }

    /**
     * RxJava範例
     * */
    void loadDataByRetrofitRxjava(HashMap<String, String> hashMap) {
        mvpView.showLoading();
        //call api method with rxjava example
        apiServices.getNotificationCountWithRxJava("/api/Android/NotifiListCount",hashMap)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RxManager<MainModel>() {
            @Override
            public void _onNext(MainModel bean) {
                mvpView.getNotificationData(bean);
                mvpView.showMessage(bean.isResult()?bean.getResponse(): (String) bean.getErrorMessage());
            }

            @Override
            public void _onError(String msg) {
                mvpView.showMessage(msg);
            }

            @Override
            public void _onCompleted() {
                mvpView.hideLoading();
                detachView();
            }
        });
    }

    /**
     * Retrofit post example
     * */
    void loadDataByRetrofitPost(HashMap<String, String> hashMap) {
        mvpView.showLoading();
        //call api method
        apiServices.getNotificationCount("api/Android/NotifiListCount", hashMap).enqueue(new Callback<MainModel>() {
            @Override
            public void onResponse(Call<MainModel> call, Response<MainModel> response) {
                mvpView.getRetrofitPost(response.body());
                mvpView.showMessage(response.body().isResult()?response.body().getResponse(): (String) response.body().getErrorMessage());
                mvpView.hideLoading();
            }

            @Override
            public void onFailure(Call<MainModel> call, Throwable t) {
                mvpView.showMessage(t.getMessage());
                mvpView.hideLoading();
            }
        });
    }

    /**
     * Retrofit post example
     * */
    void loadDataByRetrofitCombine(HashMap<String, String> hashMap) {
        mvpView.showLoading();
        //轉換URL給定完整的URL網址
        apiServices.getWeatherWithParameters("http://op.juhe.cn/onebox/weather/query?",hashMap)
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new RxManager<WeatherDataBean>() {

                @Override
                public void _onNext(WeatherDataBean model) {
                    Log.i("TAG", "@@@@@@@@@@@");
                }

                @Override
                public void _onError(String msg) {
                    mvpView.showMessage(msg);
                }

                @Override
                public void _onCompleted() {
                    mvpView.showMessage("@@@@@@@@@@@@");
                }
           });
    }
}
