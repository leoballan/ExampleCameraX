package com.vila.cameraxtest

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _barcodeList = MutableLiveData(mutableListOf<Barcode>())
    val barcodeList: LiveData<MutableList<Barcode>> = _barcodeList
    private val _barcodeListOriginal = listOf<Barcode>(
        Barcode("9789874979148"), Barcode("9789874979162"),
        Barcode("75829241"), Barcode("6942148917796"),
        Barcode("74781999"), Barcode("7345674574585")
    )
    private val _barcodeListTemp = mutableListOf<String>()


    fun insertBarcode(code: String) {
        val listTemp = _barcodeList.value
        listTemp!!.add(Barcode(code))
        _barcodeList.value = listTemp!!
    }

    fun eraseBarcode(position: Int) {
        val listTemp = _barcodeList.value
        listTemp!!.removeAt(position)
        _barcodeList.value = listTemp!!

    }

    fun processBarcode(code: String) {

        val listemp = _barcodeList.value
        if (!listemp!!.contains(Barcode(code))){
            _barcodeListTemp.add(code)
            if (_barcodeListTemp.count()>= 5){
                checkLastScannerCodes(code)
            }
        }

    }


    private fun checkLastScannerCodes(code: String) {
        val count = _barcodeListTemp.takeLast(5).filter { it == code }.count()
        Log.d("webservice","cantidad de coincidencias ..... $count")

        if ( count == 5 ){
            _barcodeListTemp.clear()
            checkBarcodeIsInOriginalList(code)
        }
    }

    private fun checkBarcodeIsInOriginalList(code: String) {
        if (_barcodeListOriginal.contains(Barcode(code)))
            insertBarcode(code)
    }


}