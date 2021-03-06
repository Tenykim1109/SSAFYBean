package com.ssafy.smartstore.fragment

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.ssafy.smartstore.activity.MainActivity
import com.ssafy.smartstore.adapter.MenuAdapter
import com.ssafy.smartstore.config.ApplicationClass.Companion.requiredPermissions
import com.ssafy.smartstore.databinding.FragmentOrderBinding
import com.ssafy.smartstore.dto.Product
import com.ssafy.smartstore.service.ProductService
import com.ssafy.smartstore.util.RetrofitCallback
import java.lang.Math.*
import kotlin.math.pow


// 하단 주문 탭
private const val TAG = "OrderFragment_싸피"
class OrderFragment : Fragment() {
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var prodList:List<Product>
    private lateinit var filteredList:MutableList<Product>
    private lateinit var binding:FragmentOrderBinding
    private val locationManager by lazy {
        context?.getSystemService(LOCATION_SERVICE) as LocationManager
    }
    companion object {
        val DEFAULT_LOCATION = LatLng(37.62064003133028, 126.9161908472352)
        var myLat = 0.1
        var myLong = 0.1
        var distance = 0
    }
    object DistanceManager {

        private const val R = 6372.8 * 1000
        fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
            val c = 2 * asin(sqrt(a))
            return (R * c).toInt()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()

        distance = DistanceManager.getDistance(myLat, myLong, DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude)
        if(checkPermission() == false)
            binding.distanceInfo.setText("매장 위치 확인하기!")
        else if(myLat == 0.1 && myLong == 0.1)
            binding.distanceInfo.setText("매장까지의 거리를 파악하고 있습니다.. ")
        else
            binding.distanceInfo.setText("매장과의 거리가 ${distance}m 입니다. ")

        binding.floatingBtn.setOnClickListener{
            //장바구니 이동
            mainActivity.openFragment(1)
        }

        binding.btnMap.setOnClickListener{
            mainActivity.openFragment(4)
        }

        binding.searchMenu.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                var searchText = binding.searchMenu.text.toString()
                searchFilter(searchText)
            }

        })

    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(requireContext(),
            requiredPermissions[0]) == PackageManager.PERMISSION_GRANTED)
    }

    private fun searchFilter(searchText:String?){
        filteredList= mutableListOf()

        for (i in 0 until prodList.size) {
            if (prodList.get(i).name.toLowerCase()
                    .contains(searchText!!.toLowerCase())
            ) {
                filteredList.add(prodList.get(i))
            }
        }

        menuAdapter.filterList(filteredList)
    }
    private fun initData(){
        ProductService().getProductList(ProductCallback())
    }

    inner class ProductCallback: RetrofitCallback<List<Product>> {
        override fun onSuccess( code: Int, productList: List<Product>) {
            productList.let {
                Log.d(TAG, "onSuccess: ${productList}")
                prodList= productList
                menuAdapter = MenuAdapter(productList)
                menuAdapter.setItemClickListener(object : MenuAdapter.ItemClickListener{
                    override fun onClick(view: View, position: Int, productId:Int) {
                        mainActivity.openFragment(3, "productId", productId)
                    }
                })
            }

            binding.recyclerViewMenu.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = menuAdapter
                //원래의 목록위치로 돌아오게함
                adapter!!.stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            Log.d(TAG, "ProductCallback: $productList")
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "유저 정보 불러오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }
    }


    private val listener = object : LocationListener {
        //위치가 변경될때 호출될 method
        @SuppressLint("SetTextI18n")  //원래는 settext할 때 xml에 string 선언해야하는데 이것을 붙이면 노란 게 없어짐
        override fun onLocationChanged(location: Location) {//location 변경될 때마다 이게 불림
            when(location.provider) {
                LocationManager.GPS_PROVIDER -> {
                    myLat = location.latitude
                    myLong = location.longitude
                    distance = DistanceManager.getDistance(location.latitude, location.longitude, DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude)
                    binding.distanceInfo.setText("매장과의 거리가 ${distance}m 입니다. ")
                    Log.d("$TAG GPS : ", "${location.latitude}/${location.longitude}")
                }
                LocationManager.NETWORK_PROVIDER -> {
                    myLat = location.latitude
                    myLong = location.longitude
                    distance = DistanceManager.getDistance(location.latitude, location.longitude, DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude)
                    binding.distanceInfo.setText("매장과의 거리가 ${distance}m 입니다. ")
                    Log.d("$TAG NET : ", "${location.latitude}/${location.longitude}")
                }
            }
        }

        override fun onProviderDisabled(provider: String) {
        }

        @SuppressLint("MissingPermission")
        override fun onProviderEnabled(provider: String) {
            if (isPermitted()) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("WrongConstant", "NewApi")
    private fun isPermitted():Boolean  {
        return context?.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }


    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if(isPermitted()){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, listener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, listener)
        }
    }

    override fun onPause() {
        super.onPause()
        if(isPermitted()) {
            locationManager.removeUpdates(listener)
        }
    }
}