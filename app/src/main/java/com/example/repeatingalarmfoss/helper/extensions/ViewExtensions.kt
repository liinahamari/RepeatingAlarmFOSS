package com.example.repeatingalarmfoss.helper.extensions

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import es.dmoral.toasty.Toasty

fun ViewGroup.inflate(layoutId: Int): View = LayoutInflater.from(context).inflate(layoutId, this, false)

fun Context.toast(message: String) = Toasty.error(this, message, Toast.LENGTH_LONG, true).show();
fun Fragment.toast(message: String) = context?.toast(message)
fun Activity.toast(message: String) = applicationContext.toast(message)

fun DialogFragment.show(fragmentManager: FragmentManager) = show(fragmentManager, this::class.java.simpleName)