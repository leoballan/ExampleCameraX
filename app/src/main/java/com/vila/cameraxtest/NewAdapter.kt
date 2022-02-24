package com.vila.cameraxtest

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vila.cameraxtest.databinding.ItemBarcodeBinding

class NewAdapter(var list:MutableList<Barcode>): RecyclerView.Adapter<NewAdapter.BarcodeViewHolder>() {




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val binding  = ItemBarcodeBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return BarcodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    fun updateList(listTemp:List<Barcode>){
        list.clear()
        list.addAll(listTemp)
        notifyDataSetChanged()
    }

    inner class BarcodeViewHolder(val binding: ItemBarcodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barcode: Barcode){
            Log.d("webservice","dentro del aadpter")
            binding.codigo.text = barcode.code
        }
    }
}