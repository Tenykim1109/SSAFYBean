package com.ssafy.smartstore.service

import android.os.Build
import androidx.annotation.RequiresApi
import com.ssafy.smartstore.dto.User
import com.ssafy.smartstore.util.RetrofitCallback
import com.ssafy.smartstore.util.RetrofitUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "LoginService_싸피"
class UserService {



    fun login(user:User, callback: RetrofitCallback<User>)  {
        RetrofitUtil.userService.login(user).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                val res = response.body()
                if(response.code() == 200){
                    if (res != null) {
                        callback.onSuccess(response.code(), res)
                    }
                } else {
                    callback.onFailure(response.code())
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    fun join(user:User, callback: RetrofitCallback<Boolean>)  {
        RetrofitUtil.userService.insert(user).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                val res = response.body()
                if(response.code() == 200){
                    if (res != null) {
                        callback.onSuccess(response.code(), res)
                    }
                } else {
                    callback.onFailure(response.code())
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    fun isUsedId(id:String, callback: RetrofitCallback<Boolean>)  {
        RetrofitUtil.userService.isUsedId(id).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                val res = response.body()
                if(response.code() == 200){
                    if (res != null) {
                        callback.onSuccess(response.code(), res)
                    }
                } else {
                    callback.onFailure(response.code())
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    fun getInfo(id: String, callback: RetrofitCallback<HashMap<String, Any>>) {
        RetrofitUtil.userService.getInfo(id).enqueue(object : Callback<HashMap<String, Any>> {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onResponse(
                call: Call<HashMap<String, Any>>,
                response: Response<HashMap<String, Any>>
            ) {
                val res = response.body()

                if (response.code() == 200) {
                    if (res != null) {
                        callback.onSuccess(response.code(), res)
                    } else {
                        callback.onFailure(response.code())
                    }
                }
            }

            override fun onFailure(call: Call<HashMap<String, Any>>, t: Throwable) {
                callback.onError(t)
            }
        })
    }
}