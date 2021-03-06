package com.ssafy.smartstore.activity

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ssafy.smartstore.*
import com.ssafy.smartstore.R
import com.ssafy.smartstore.adapter.OrderDetailListAdapter
import com.ssafy.smartstore.config.ApplicationClass
import com.ssafy.smartstore.config.ShoppingListViewModel
import com.ssafy.smartstore.dto.Order
import com.ssafy.smartstore.dto.OrderDetail
import com.ssafy.smartstore.fragment.*
import com.ssafy.smartstore.fragment.ShoppingListFragment.Companion.nd
import com.ssafy.smartstore.service.CouponService
import com.ssafy.smartstore.service.OrderService
import com.ssafy.smartstore.util.RetrofitCallback
import org.altbeacon.beacon.*
import java.util.*

private const val TAG = "MainActivity_??????"
class MainActivity : AppCompatActivity(), BeaconConsumer {
    private lateinit var bottomNavigation : BottomNavigationView
    val shoppingListViewModel: ShoppingListViewModel by lazy {
        ViewModelProvider(this)[ShoppingListViewModel::class.java]
    }
    var tableN = ""
    var orderId = -1
    var userCouponId = -1
    var readable = false
    var isNear = false

    // beacon
    private lateinit var beaconManager: BeaconManager
    private val BEACON_UUID = "fda50693-a4e2-4fb1-afcf-c6eb07647825"
    private val BEACON_MAJOR = "10004"
    private val BEACON_MINOR = "54480"
    private val STORE_DISTANCE = 30 //????????? ?????? 30m

    private val region = Region("altbeacon"
        , Identifier.parse(BEACON_UUID)
        , Identifier.parse(BEACON_MAJOR)
        , Identifier.parse(BEACON_MINOR)
    )

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var needBLERequest = true

    private val PERMISSIONS_CODE = 100

    // nfc
    var nfcAdapter: NfcAdapter? = null
    var pIntent:PendingIntent? = null
    lateinit var filters: Array<IntentFilter>
    lateinit var i:Intent

    private lateinit var locationRequest: LocationRequest
    private val UPDATE_INTERVAL = 1000 // 1 ???
    private val FASTEST_UPDATE_INTERVAL = 500 // 0.5 ???
    private var mFusedLocationClient: FusedLocationProviderClient? = null


    // Intent ?????? requestForActivity ??????
    private val requestActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult() // ??? StartActivityForResult ????????? ??????
    ) {
        // ???????????? GPS ??? ????????? ?????????
        if (checkLocationServicesStatus() == false) {
            Log.d(TAG, "dialog: ")
            showDialogForLocationServiceSetting()

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // ?????? ??? ????????? ??? ????????? Fragment??? ??????

        checkPermissions()

        setNdef()

        setBeacon()

        createNotificationChannel("ssafy_channel", "ssafy")

        setLocationRequest()


        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout_main, HomeFragment())
            .commit()

        bottomNavigation = findViewById(R.id.tab_layout_bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when(item.itemId){
                R.id.navigation_page_1 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout_main, HomeFragment())
                        .commit()
                    true
                }
                R.id.navigation_page_2 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout_main, OrderFragment())
                        .commit()
                    true
                }
                R.id.navigation_page_3 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout_main, FavoriteFragment())
                        .commit()
                    true
                }
                R.id.navigation_page_4 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout_main, MyPageFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        bottomNavigation.setOnNavigationItemReselectedListener { item ->
            // ???????????? ?????? ????????? ?????? ?????? ?????? ??????
            if(bottomNavigation.selectedItemId != item.itemId){
                bottomNavigation.selectedItemId = item.itemId
            }
        }
    }

    fun openFragment(index:Int, key:String, value:Int){
        moveFragment(index, key, value)
    }

    fun openFragment(index: Int) {
        moveFragment(index, "", 0)
    }

    private fun moveFragment(index:Int, key:String, value:Int){
        val transaction = supportFragmentManager.beginTransaction()
        when(index){
            //????????????
            1 -> {
                if (value != 0) {
                    transaction.replace(R.id.frame_layout_main, ShoppingListFragment.newInstance(key, value))
                        .addToBackStack(null)
                } else {
                    transaction.replace(R.id.frame_layout_main, ShoppingListFragment())
                        .addToBackStack(null)
                }
            }
            //?????? ?????? ??????
            2 -> transaction.replace(R.id.frame_layout_main, OrderDetailFragment.newInstance(key, value))
                .addToBackStack(null)
            //?????? ?????? ??????
            3 -> transaction.replace(R.id.frame_layout_main, MenuDetailFragment.newInstance(key, value))
                .addToBackStack(null)
            //map?????? ??????
            4 -> transaction.replace(R.id.frame_layout_main, MapFragment())
                .addToBackStack(null)
            //logout
            5 -> {
                logout()
            }
            //coupon
            6 -> transaction.replace(R.id.frame_layout_main, CouponFragment())
                .addToBackStack(null)
        }
        transaction.commit()
    }

    fun logout(){
        //preference ?????????
        ApplicationClass.sharedPreferencesUtil.deleteUser()

        //????????????
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent)
    }

    fun hideBottomNav(state : Boolean){
        if(state) bottomNavigation.visibility =  View.GONE
        else bottomNavigation.visibility = View.VISIBLE
    }

    private fun setNdef(){
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            finish()
        }
        i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        pIntent = PendingIntent.getActivity(this, 0, i, 0)

        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        filters = arrayOf(tagFilter)
    }

    private fun setBeacon(){
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        startScan()
    }

    // NotificationChannel ??????
    private fun createNotificationChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(id, name, importance)

            val notificationManager: NotificationManager
                    = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ?????? ??????
    private fun checkPermissions(){
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                this,
                ApplicationClass.requiredPermissions,
                PERMISSIONS_CODE
            )
        }
    }

    // ?????? ?????? ??????
    private fun setLocationRequest() {
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting()
        }

        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL.toLong()
            smallestDisplacement = 10.0f
            fastestInterval = FASTEST_UPDATE_INTERVAL.toLong()
        }

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // Beacon Scan ??????
    private fun startScan() {
        // ???????????? Enable ??????
        if(!isEnableBLEService()){
            requestEnableBLE()
            Log.d(TAG, "startScan: ??????????????? ????????? ???????????????.")
            return
        }

        // ?????? ?????? ?????? ?????? ??? GPS Enable ?????? ??????
        checkPermissions()
        Log.d(TAG, "startScan: beacon Scan start")

        // Beacon Service bind
        beaconManager.bind(this)
    }

    // ???????????? ????????? ??????
    private fun isEnableBLEService(): Boolean{
        if(!bluetoothAdapter!!.isEnabled){
            return false
        }
        return true
    }

    // ???????????? ON/OFF ?????? ?????? ??? ????????? ?????? ??????
    private fun requestEnableBLE(){
        val callBLEEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestBLEActivity.launch(callBLEEnableIntent)
        Log.d(TAG, "requestEnableBLE: ")
    }

    private val requestBLEActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        // ???????????? ???????????? ????????? ???????????? ??????
        if (isEnableBLEService()) {
            needBLERequest = false
            startScan()
        }
    }

    // ?????? ?????? ?????? ?????? ?????? ?????? ??????
    // ActivityCompat.requestPermissions ?????? ?????? ??????
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_CODE -> {
                if(grantResults.isNotEmpty()) {
                    for((i, permission) in permissions.withIndex()) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            //?????? ?????? ??????
                            Log.i(TAG, "$permission ?????? ????????? ?????????????????????.")
//                            finish()
                        }
                    }
                }
            }
        }
    }
    override fun onBeaconServiceConnect() {

        beaconManager.addMonitorNotifier(object : MonitorNotifier {

            override fun didEnterRegion(region: Region?) {
                try {
                    Log.d(TAG, "????????? ?????????????????????.------------${region.toString()}")
                    beaconManager.startRangingBeaconsInRegion(region!!)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didExitRegion(region: Region?) {
                try {
                    Log.d(TAG, "????????? ?????? ??? ????????????.")
                    beaconManager.stopRangingBeaconsInRegion(region!!)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didDetermineStateForRegion(i: Int, region: Region?) {}
        })

        beaconManager.addRangeNotifier { beacons, region ->
            for (beacon in beacons) {
                // Major, Minor??? Beacon ??????, 30?????? ????????? ???????????? ??????????????? ??????
                if(isStoreBeacon(beacon)){
                    isNear = true
                    if (beacon.distance <= STORE_DISTANCE) {
                        showPopDialog()
                        beaconManager.stopMonitoringBeaconsInRegion(region)
                        beaconManager.stopRangingBeaconsInRegion(region)
                    }
                }
            }
        }

        try {
            beaconManager.startMonitoringBeaconsInRegion(region)
        } catch (e: RemoteException){
            e.printStackTrace()
        }
    }

    // ????????? ?????? Beacon??? ?????????, ????????? ?????? ???????????? ??????
    private fun isStoreBeacon(beacon: Beacon): Boolean {
        return beacon.id2.toString() == BEACON_MAJOR && beacon.id3.toString() == BEACON_MINOR

    }

    private fun showPopDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_popup, null)

        if (orderId > 0) {
            val orderList = OrderService().getOrderDetails(orderId)
            orderId = -1    // ????????? ????????????, pickUp ???????????? ???????????? ??????

            runOnUiThread {
                orderList.observe(this, { orderDetails ->
                    val orderListAdapter = OrderDetailListAdapter(this, orderDetails)
                    view.apply {
                        findViewById<TextView>(R.id.tvVoid).visibility = View.GONE
                        findViewById<RecyclerView>(R.id.dialogRecycler).apply {
                            val linearLayoutManager = LinearLayoutManager(context)
                            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                            layoutManager = linearLayoutManager
                            adapter = orderListAdapter
                        }
                    }
                    AlertDialog.Builder(this).apply {
                        setView(view)
                        setPositiveButton("??????", null)
                        show()
                    }
                    Log.d(TAG, "showDialog: success")
                })
            }
        } else {
            runOnUiThread {
                view.apply {
                    findViewById<TextView>(R.id.tvVoid).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.dialogRecycler).visibility = View.GONE
                }
                AlertDialog.Builder(this).apply {
                    setView(view)
                    setPositiveButton("??????", null)
                    show()
                }
            }
            Log.d(TAG, "showDialog: success")
        }
    }

    //Tag ???????????? ???????????? ??????
    private fun getNFCData(intent: Intent){
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        if(rawMsgs!=null){
            nd!!.dismiss()
            val message = arrayOfNulls<NdefMessage>(rawMsgs.size)
            for(i in rawMsgs.indices){
                message[i]=rawMsgs[i] as NdefMessage
            }
            //?????? ???????????? ?????? ???????????? ??????
            val record_data = message[0]!!.records[0]
            val record_type = record_data.type
            val type = String(record_type)
            if(type.equals("T")){
                val data = record_data.payload
                //????????? ???????????? TextView??? ??????
                tableN = String(data).substring(3)
                Log.d("tableN", "getNFCData: $tableN")
                completedOrder()
            }
        }
    }

    fun completedOrder() {
        shoppingListViewModel.shoppingList.observe(this, { list ->
            Log.d(TAG, "completedOrder: $list")

            val order = Order().apply {
                userId = ApplicationClass.sharedPreferencesUtil.getUser().id
                orderTable = tableN
            }

            for (item in list) {
                order.details.add(OrderDetail(item.menuId, item.menuCnt))
            }

            OrderService().makeOrder(order, OrderCallback())
        })
    }

    private fun couponStateUpdate() {
        if (userCouponId > 0)
            CouponService().updateCouponUsed(userCouponId, CouponStateCallback())
    }

    // GPS ??????????????? ??????
    fun checkLocationServicesStatus(): Boolean {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    private fun showDialogForLocationServiceSetting() {
        val builder: androidx.appcompat.app.AlertDialog.Builder =
            androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("?????? ????????? ????????????")
        builder.setMessage(
            "?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
        )
        builder.setCancelable(true)
        builder.setPositiveButton("??????") { _, _ ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            requestActivity.launch(callGPSSettingIntent)
        }
        builder.setNegativeButton(
            "??????"
        ) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    inner class OrderCallback: RetrofitCallback<Int> {
        override fun onSuccess(code: Int, responseData: Int) {
            couponStateUpdate()
            orderId = responseData
            Toast.makeText(this@MainActivity, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
            readable = false
            shoppingListViewModel.clearCart()
            supportFragmentManager.apply {
                beginTransaction().remove(ShoppingListFragment()).commit()
                popBackStack()
                beginTransaction().replace(R.id.frame_layout_main, MyPageFragment()).commit()
            }
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "???????????? ???????????? ??? ????????????")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }
    }

    inner class CouponStateCallback : RetrofitCallback<Boolean> {
        override fun onSuccess(code: Int, responseData: Boolean) {
            if (responseData) {
                Log.d(TAG, "onSuccess: ?????? ?????? ??????")
                userCouponId = -1
            }
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "???????????? ???????????? ??? ????????????")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }

    }

    override fun onDestroy() {
        beaconManager.stopMonitoringBeaconsInRegion(region)
        beaconManager.stopRangingBeaconsInRegion(region)
        beaconManager.unbind(this)
        super.onDestroy()

    }

    override fun onResume() {
        super.onResume()
        nfcAdapter!!.enableForegroundDispatch(this, pIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter!!.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.e("INFO", "onNewIntent called...")
        Log.e(TAG, "${intent!!.action}", )

        if(readable == true && intent.action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)){
            getNFCData(intent)
        }
    }
}