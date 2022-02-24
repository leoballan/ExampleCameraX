package com.vila.cameraxtest

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.vila.cameraxtest.databinding.ItemBarcodeBinding

class Adapter : ListAdapter<Barcode, Adapter.BarcodeViewHolder>(BarcodeDiffUtil) {

    inner class BarcodeViewHolder(val binding: ItemBarcodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(barcode: Barcode){
                Log.d("webservice","dentro del aadpter")
                binding.codigo.text = barcode.code
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val binding = ItemBarcodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BarcodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object BarcodeDiffUtil : DiffUtil.ItemCallback<Barcode>() {
        override fun areItemsTheSame(oldItem: Barcode, newItem: Barcode) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: Barcode, newItem: Barcode): Boolean {
            return oldItem.code == newItem.code
        }
    }

}