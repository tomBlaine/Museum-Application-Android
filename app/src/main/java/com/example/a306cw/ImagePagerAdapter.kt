import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.a306cw.R

class ImagePagerAdapter(private val context: Context, private val imageUris: ArrayList<String>) : PagerAdapter() {

    override fun getCount(): Int {
        return imageUris.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.image_view_pager, container, false)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        Glide.with(context)
            .load(imageUris[position])
            .apply(RequestOptions().placeholder(R.drawable.placeholder))
            .into(imageView)

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}