package com.tunjid.androidbootstrap.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import com.tunjid.androidbootstrap.PlaceHolder
import com.tunjid.androidbootstrap.R
import com.tunjid.androidbootstrap.adapters.DoggoAdapter
import com.tunjid.androidbootstrap.adapters.withPaddedAdapter
import com.tunjid.androidbootstrap.baseclasses.AppBaseFragment
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment
import com.tunjid.androidbootstrap.fragments.AdoptDoggoFragment.Companion.ARG_DOGGO
import com.tunjid.androidbootstrap.model.Doggo
import com.tunjid.androidbootstrap.recyclerview.ListManager
import com.tunjid.androidbootstrap.recyclerview.ListManager.SWIPE_DRAG_ALL_DIRECTIONS
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.androidbootstrap.view.util.InsetFlags
import com.tunjid.androidbootstrap.view.util.ViewUtil
import com.tunjid.androidbootstrap.viewholders.DoggoRankViewHolder
import com.tunjid.androidbootstrap.viewholders.DoggoViewHolder
import com.tunjid.androidbootstrap.viewmodels.DoggoRankViewModel
import kotlin.math.abs

class DoggoRankFragment : AppBaseFragment(), DoggoAdapter.ImageListAdapterListener {

    private lateinit var viewModel: DoggoRankViewModel
    private lateinit var listManager: ListManager<DoggoRankViewHolder, PlaceHolder.State>

    override val fabIconRes: Int = R.drawable.ic_restore_24dp

    override val fabText: CharSequence
        get() = getString(R.string.reset_doggos)

    override val fabClickListener: View.OnClickListener
        get() = View.OnClickListener { viewModel.resetList() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DoggoRankViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_simple_list, container, false)
        val placeHolder = PlaceHolder(root.findViewById(R.id.placeholder_container))

        listManager = ListManagerBuilder<DoggoRankViewHolder, PlaceHolder.State>()
                .withRecyclerView(root.findViewById(R.id.recycler_view))
                .withPaddedAdapter(DoggoAdapter(
                        viewModel.doggos,
                        R.layout.viewholder_doggo_rank,
                        { itemView, adapterListener -> DoggoRankViewHolder(itemView, adapterListener) },
                        this))
                .addScrollListener { _, dy -> if (abs(dy!!) > 4) isFabExtended = dy < 0 }
                .withPlaceholder(placeHolder)
                .withLinearLayoutManager()
                .withSwipeDragOptions(ListManager.swipeDragOptionsBuilder<DoggoRankViewHolder>()
                        .setMovementFlagsFunction { SWIPE_DRAG_ALL_DIRECTIONS }
                        .setSwipeConsumer { holder, _ -> removeDoggo(holder) }
                        .setDragHandleFunction { it.dragView }
                        .setSwipeDragStartConsumer { holder, actionState -> this.onSwipeOrDragStarted(holder, actionState) }
                        .setSwipeDragEndConsumer { viewHolder, actionState -> this.onSwipeOrDragEnded(viewHolder, actionState) }
                        .setLongPressDragEnabledSupplier { false }
                        .setItemViewSwipeSupplier { true }
                        .setDragConsumer { start, end -> this.moveDoggo(start, end) }
                        .build())
                .build()

        postponeEnterTransition()

        return root
    }

    override fun onResume() {
        super.onResume()
        disposables.add(viewModel.watchDoggos().subscribe(this::onDiff, Throwable::printStackTrace))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listManager.clear()
    }

    override val insetFlags: InsetFlags
        get() = NO_BOTTOM

    override fun onDoggoClicked(doggo: Doggo) {
        Doggo.setTransitionDoggo(doggo)
        showFragment(AdoptDoggoFragment.newInstance(doggo))
    }

    override fun onDoggoImageLoaded(doggo: Doggo) {
        if (doggo == Doggo.getTransitionDoggo()) startPostponedEnterTransition()
    }

    override val showsFab: Boolean
        get() {
            return true
        }

    @SuppressLint("CommitTransaction")
    override fun provideFragmentTransaction(fragmentTo: BaseFragment): FragmentTransaction? {
        if (!fragmentTo.stableTag.contains(AdoptDoggoFragment::class.java.simpleName)) return null

        val args = fragmentTo.arguments ?: return null
        val doggo = args.getParcelable<Doggo>(ARG_DOGGO) ?: return null
        val holder = listManager.findViewHolderForItemId(doggo.hashCode().toLong()) ?: return null

        return requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .addSharedElement(holder.thumbnail, ViewUtil.transitionName(doggo, holder.thumbnail))
    }

    private fun onDiff(diffResult: DiffUtil.DiffResult) {
        listManager.onDiff(diffResult)
        togglePersistentUi()
    }

    private fun moveDoggo(start: DoggoViewHolder, end: DoggoViewHolder) {
        val from = start.adapterPosition
        val to = end.adapterPosition

        viewModel.swap(from, to)
        listManager.notifyItemMoved(from, to)
        listManager.notifyItemChanged(from)
        listManager.notifyItemChanged(to)
    }

    private fun removeDoggo(viewHolder: DoggoViewHolder) {
        val position = viewHolder.adapterPosition
        val minMax = viewModel.remove(position)

        listManager.notifyItemRemoved(position)
        // Only necessary to rebind views lower so they have the right position
        listManager.notifyItemRangeChanged(minMax.first, minMax.second)
    }

    private fun onSwipeOrDragStarted(holder: DoggoRankViewHolder, actionState: Int) {
        viewModel.onActionStarted(Pair(holder.itemId, actionState))
    }

    private fun onSwipeOrDragEnded(viewHolder: DoggoViewHolder, actionState: Int) {
        val message = viewModel.onActionEnded(Pair(viewHolder.itemId, actionState))
        if (!TextUtils.isEmpty(message)) showSnackbar { snackBar -> snackBar.setText(message) }
    }

    companion object {
        fun newInstance(): DoggoRankFragment = DoggoRankFragment().apply { arguments = Bundle() }
    }
}
