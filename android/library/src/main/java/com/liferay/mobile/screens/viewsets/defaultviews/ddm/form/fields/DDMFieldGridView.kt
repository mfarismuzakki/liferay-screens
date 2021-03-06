/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.mobile.screens.viewsets.defaultviews.ddm.form.fields

import android.content.Context
import android.graphics.Typeface
import com.google.android.material.snackbar.Snackbar
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.liferay.mobile.screens.R
import com.liferay.mobile.screens.ddl.form.view.DDLFieldViewModel
import com.liferay.mobile.screens.ddl.model.Option
import com.liferay.mobile.screens.ddm.form.model.Grid
import com.liferay.mobile.screens.ddm.form.model.GridField
import com.liferay.mobile.screens.ddm.form.model.get
import com.liferay.mobile.screens.delegates.bindNonNull
import com.liferay.mobile.screens.util.extensions.forEachChild
import com.liferay.mobile.screens.util.AndroidUtil
import com.liferay.mobile.screens.viewsets.defaultviews.util.ThemeUtil
import rx.Observable
import rx.Subscriber
import rx.Subscription

/**
 * @author Victor Oliveira
 */
open class DDMFieldGridView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
	defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr), DDLFieldViewModel<GridField> {

	private lateinit var gridField: GridField
	private lateinit var parentView: View

	private val labelTextView: TextView by bindNonNull(R.id.liferay_ddm_label)
	private val hintTextView: TextView by bindNonNull(R.id.liferay_ddm_hint)
	private var changeValuesSubscription: Subscription? = null

	private var changeValuesSubscriber: Subscriber<in Boolean>? = null
	private var changeValuesGridSubscriber: Subscriber<in GridField>? = null
	private val changeValuesObservable = Observable.create<Boolean> { changeValuesSubscriber = it }

	val gridLinearLayout: LinearLayout by bindNonNull(R.id.liferay_ddm_grid)

	override fun getField(): GridField {
		return gridField
	}

	override fun setField(field: GridField) {
		this.gridField = field

		setupLabelLayout()
		setupFieldLayout()
	}

	override fun getOnChangedValueObservable(): Observable<GridField> {
		return Observable.create<GridField> {
			changeValuesGridSubscriber = it
		}
	}

	override fun refresh() {
		setupLabelLayout()
		refreshGridRows()
	}

	override fun onPostValidation(valid: Boolean) {
		val errorText = if (valid) null else context.getString(R.string.required_value)

		if (field.isShowLabel) {
			val label = findViewById<TextView>(R.id.liferay_ddm_label)
			label?.error = errorText
		} else {
			Snackbar.make(this, errorText.toString(), Snackbar.LENGTH_SHORT).show()
		}
	}

	override fun getParentView(): View {
		return parentView
	}

	override fun setParentView(view: View) {
		parentView = view
	}

	override fun setUpdateMode(enabled: Boolean) {
		field.isShowLabel.let {
			val label = findViewById<TextView>(R.id.liferay_ddm_label)
			label?.isEnabled = enabled
		}

		gridLinearLayout.forEachChild { view ->
			val ddmFieldGridRowView = view as? DDMFieldGridRowView
			ddmFieldGridRowView?.also { it.columnSelectView.setUpdateMode(enabled) }
		}

		this.isEnabled = enabled
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()

		changeValuesSubscription?.unsubscribe()
	}

	private fun refreshGridRows() {
		gridLinearLayout.forEachChild { view ->
			val ddmFieldGridRowView = view as? DDMFieldGridRowView

			ddmFieldGridRowView?.also { gridRowView ->
				field.currentValue?.let { grid ->
					grid[gridRowView.rowOption.value]
				}?.let { columnValue ->
					gridField.columns[columnValue]
				}?.run {
					gridRowView.selectOption(this)
				}
			}
		}
	}

	private fun setupLabelLayout() {
		AndroidUtil.updateLabelLayout(labelTextView, gridField, context)
		AndroidUtil.updateHintLayout(hintTextView, gridField)
	}

	private fun onColumnValueChanged(which: Int, row: Option, ddmFieldGridRowView: DDMFieldGridRowView) {
		val option = this.gridField.columns[which]

		if (this.gridField.currentValue == null) {
			this.gridField.currentValue = Grid(mutableMapOf(row.value to option.value))
		} else {
			this.gridField.currentValue.rawValues[row.value] = option.value
		}

		ddmFieldGridRowView.refresh()

		changeValuesSubscriber?.onNext(field.isValid)
		changeValuesGridSubscriber?.onNext(field)
	}

	private fun setupFieldLayout() {
		val inflater = LayoutInflater.from(context)
		val layoutIdentifier = ThemeUtil.getLayoutIdentifier(context, "ddmfield_grid_row")

		this.gridField.rows.forEach { row ->
			val ddmFieldGridRowView =
				inflater.inflate(layoutIdentifier, gridLinearLayout, false) as DDMFieldGridRowView

			gridLinearLayout.addView(ddmFieldGridRowView)

			ddmFieldGridRowView.setOptions(row, gridField.columns)

			ddmFieldGridRowView.columnSelectView.apply {
				setOnValueChangedListener { _, which ->
					onColumnValueChanged(which, row, ddmFieldGridRowView)
				}

				setOnClearListener {
					ddmFieldGridRowView.refresh()
				}
			}
		}

		changeValuesSubscription = changeValuesObservable
			.filter { it }
			.distinctUntilChanged()
			.subscribe(::onPostValidation)

		refreshGridRows()
	}
}