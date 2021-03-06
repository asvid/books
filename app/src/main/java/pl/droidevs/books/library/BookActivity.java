package pl.droidevs.books.library;

import android.animation.ValueAnimator;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.transition.Transition.TransitionListener;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import pl.droidevs.books.R;
import pl.droidevs.books.domain.Book;
import pl.droidevs.books.domain.BookId;
import pl.droidevs.books.savebook.SaveBookActivity;

import static pl.droidevs.books.Resource.Status.SUCCESS;
import static pl.droidevs.books.apphelper.ColorHelper.getActionBarColorFromSwatch;
import static pl.droidevs.books.apphelper.ColorHelper.getDominantColor;
import static pl.droidevs.books.apphelper.ColorHelper.getStatusBarColorFromSwatch;

public class BookActivity extends AppCompatActivity {
    public static final String EXTRAS_BOOK_ID = "EXTRAS_BOOK_ID";
    public static final String EXTRAS_IMAGE_TRANSITION_NAME = "EXTRAS_IMAGE_TRANSITION_NAME";
    public static final String EXTRAS_TITLE_TRANSITION_NAME = "EXTRAS_TITLE_TRANSITION_NAME";
    public static final String EXTRAS_AUTHOR_TRANSITION_NAME = "EXTRAS_AUTHOR_TRANSITION_NAME";
    public static final String EXTRAS_SHADOW_TRANSITION_NAME = "EXTRAS_SHADOW_TRANSITION_NAME";
    public static final String EXTRAS_LAST_SELECTED_INDEX = "EXTRAS_LAST_SELECTED_INDEX";
    public static final String EXTRAS_SHARED_TITLE_TEXT_SIZE = "EXTRAS_SHARED_TITLE_TEXT_SIZE";
    public static final String EXTRAS_SHARED_AUTHOR_TEXT_SIZE = "EXTRAS_SHARED_AUTHOR_TEXT_SIZE";
    public static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";

    private static final int EDIT_BOOK_REQUEST_CODE = 205;

    private static final double APP_BAR_MAX_COLLEPSED_SCROLL_PERCENT_VALUE = 0.4;
    private static final double APP_BAR_MIN_EXPANDED_SCROLL_PERCENT_VALUE = 1.0 - APP_BAR_MAX_COLLEPSED_SCROLL_PERCENT_VALUE;
    private static final double APP_BAR_ALPHA_SCROLL_MAX_PERCENT_VALUE = APP_BAR_MIN_EXPANDED_SCROLL_PERCENT_VALUE - APP_BAR_MAX_COLLEPSED_SCROLL_PERCENT_VALUE;

    private static final int MAX_ALPHA = 255;
    private static final int MIN_ALPHA = 0;

    private Bundle animationBundle;

    private BookId bookId;
    private float masterTitleTextSize;
    private float masterAuthorTextSize;

    private TransitionListener transitionListener;

    private int lastAppBarOffset = 1;
    private int linesCount = 0;
    private double appBarScrollPercentValue;

    @BindView(R.id.album_iv)
    ImageView imageView;

    @BindView(R.id.collapsing_toolbar_layout)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @BindView(R.id.app_bar_layout)
    AppBarLayout appBarLayout;

    @BindView(R.id.shadow_view)
    View shadowView;

    @BindView(R.id.tv_expanded_title)
    TextView tvExpandedTitle;

    @BindView(R.id.tv_collapsed_title)
    TextView tvCollpsedTitle;

    @BindView(R.id.cl_title)
    ConstraintLayout clTitle;

    @BindView(R.id.author_tv)
    TextView authorTextView;

    @BindView(R.id.category_tv)
    TextView categoryTextView;

    @BindView(R.id.description_tv)
    TextView descriptionTextView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        AndroidInjection.inject(this);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            this.animationBundle = savedInstanceState.getBundle(BUNDLE_EXTRAS);
            this.bookId = (BookId) savedInstanceState.getSerializable(EXTRAS_BOOK_ID);
        } else {
            this.animationBundle = getIntent().getBundleExtra(BUNDLE_EXTRAS);
            this.bookId = (BookId) getIntent().getSerializableExtra(EXTRAS_BOOK_ID);
        }

        setupAnimations();
        setupAppBarLayoutOffsetListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViewModel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setTransitionListener(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_details_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.edit_book) {
            Intent intent = new Intent(this, SaveBookActivity.class);
            intent.putExtra(SaveBookActivity.BOOK_ID_EXTRA, getIntent().getSerializableExtra(EXTRAS_BOOK_ID));

            startActivityForResult(intent, EDIT_BOOK_REQUEST_CODE, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_BOOK_REQUEST_CODE &&
                resultCode == SaveBookActivity.RESULT_BOOK_REMOVED) {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle(BUNDLE_EXTRAS, animationBundle);
        outState.putSerializable(EXTRAS_BOOK_ID, bookId);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(BUNDLE_EXTRAS, animationBundle);
        setResult(RESULT_OK, intent);

        setExitTransition();

        super.onBackPressed();
    }

    private void setTransitionListener(@Nullable TransitionListener newTransitionListener) {
        if (this.transitionListener != null) {
            getWindow().getSharedElementEnterTransition().removeListener(this.transitionListener);
        }
        this.transitionListener = newTransitionListener;
        if (newTransitionListener != null) {
            getWindow().getSharedElementEnterTransition().addListener(this.transitionListener);
        }
    }

    private TransitionListener getEnterTransitionListener() {
        return new TransitionListener() {
            private float detailsTitleTextSize = -1;
            private float detailsAuthorTextSize = -1;

            @Override
            public void onTransitionStart(android.transition.Transition transition) {
                detailsAuthorTextSize = authorTextView.getTextSize();
                if (detailsAuthorTextSize >= 0) {
                    authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, masterAuthorTextSize);
                    ValueAnimator animator = ValueAnimator.ofFloat(masterAuthorTextSize, detailsAuthorTextSize);
                    animator.setDuration(250);
                    animator.addUpdateListener(valueAnimator -> {
                        float textSize = (float) valueAnimator.getAnimatedValue();
                        authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    });
                    animator.start();
                }

                detailsTitleTextSize = tvExpandedTitle.getTextSize();
                if (detailsTitleTextSize >= 0) {
                    tvExpandedTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, masterTitleTextSize);
                    ValueAnimator animator = ValueAnimator.ofFloat(masterTitleTextSize, detailsTitleTextSize);
                    animator.setDuration(250);
                    animator.addUpdateListener(valueAnimator -> {
                        float textSize = (float) valueAnimator.getAnimatedValue();
                        tvExpandedTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    });
                    animator.start();
                }
                ValueAnimator titleColorAnimator = ValueAnimator.ofArgb(R.color.defaultTextViewTextColor, Color.WHITE);
                titleColorAnimator.setDuration(250);
                titleColorAnimator.addUpdateListener(valueAnimator -> {
                    int color = (int) valueAnimator.getAnimatedValue();
                    tvExpandedTitle.setTextColor(color);
                });
                titleColorAnimator.start();
            }

            @Override
            public void onTransitionEnd(android.transition.Transition transition) {
                if (detailsAuthorTextSize >= 0) {
                    authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailsAuthorTextSize);
                }
                if (detailsTitleTextSize >= 0) {
                    tvExpandedTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailsTitleTextSize);
                }
            }

            @Override
            public void onTransitionCancel(android.transition.Transition transition) {
            }

            @Override
            public void onTransitionPause(android.transition.Transition transition) {
            }

            @Override
            public void onTransitionResume(android.transition.Transition transition) {
            }
        };
    }

    private TransitionListener getExitTransitionListener(TextView tvTitle) {
        return new TransitionListener() {
            private float detailsTitleTextSize = -1;
            private float detailsAuthorTextSize = -1;

            @Override
            public void onTransitionStart(android.transition.Transition transition) {
                detailsAuthorTextSize = authorTextView.getTextSize();
                if (detailsAuthorTextSize >= 0) {
                    authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailsAuthorTextSize);
                    ValueAnimator animator = ValueAnimator.ofFloat(detailsAuthorTextSize, masterAuthorTextSize);
                    animator.setDuration(250);
                    animator.addUpdateListener(valueAnimator -> {
                        float textSize = (float) valueAnimator.getAnimatedValue();
                        authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    });
                    animator.start();
                }

                detailsTitleTextSize = tvTitle.getTextSize();
                if (detailsTitleTextSize >= 0) {
                    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailsTitleTextSize);
                    ValueAnimator animator = ValueAnimator.ofFloat(detailsTitleTextSize, masterTitleTextSize);
                    animator.setDuration(250);
                    animator.addUpdateListener(valueAnimator -> {
                        float textSize = (float) valueAnimator.getAnimatedValue();
                        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    });
                    animator.start();
                }
                int defColor;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    defColor = getResources().getColor(R.color.defaultTextViewTextColor, getTheme());
                } else {
                    defColor = getResources().getColor(R.color.defaultTextViewTextColor);
                }
                ValueAnimator titleColorAnimator = ValueAnimator.ofArgb(Color.WHITE, defColor);
                titleColorAnimator.setDuration(250);
                titleColorAnimator.addUpdateListener(valueAnimator -> {
                    int color = (int) valueAnimator.getAnimatedValue();
                    tvTitle.setTextColor(color);
                });
                titleColorAnimator.start();
            }

            @Override
            public void onTransitionEnd(android.transition.Transition transition) {
                if (detailsAuthorTextSize >= 0) {
                    authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, masterAuthorTextSize);
                }
                if (detailsTitleTextSize >= 0) {
                    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, masterTitleTextSize);
                }
            }

            @Override
            public void onTransitionCancel(android.transition.Transition transition) {
            }

            @Override
            public void onTransitionPause(android.transition.Transition transition) {
            }

            @Override
            public void onTransitionResume(android.transition.Transition transition) {
            }
        };
    }

    private void setExitTransition() {
        if (appBarScrollPercentValue < APP_BAR_MAX_COLLEPSED_SCROLL_PERCENT_VALUE) {
            tvExpandedTitle.setTransitionName("");
            tvCollpsedTitle.setTransitionName(animationBundle.getString(EXTRAS_TITLE_TRANSITION_NAME));

            int ivHeight = imageView.getHeight();
            int tHeight = toolbar.getHeight();
            int ivPadding = -((ivHeight - tHeight) / 2);
            imageView.setTranslationY(ivPadding);

            setTransitionListener(getExitTransitionListener(tvCollpsedTitle));
        } else {
            tvCollpsedTitle.setTransitionName("");
            tvExpandedTitle.setTransitionName(animationBundle.getString(EXTRAS_TITLE_TRANSITION_NAME));
            setTransitionListener(getExitTransitionListener(tvExpandedTitle));
        }
    }

    void setupAnimations() {
        imageView.setTransitionName(animationBundle.getString(EXTRAS_IMAGE_TRANSITION_NAME));
        tvExpandedTitle.setTransitionName(animationBundle.getString(EXTRAS_TITLE_TRANSITION_NAME));
        authorTextView.setTransitionName(animationBundle.getString(EXTRAS_AUTHOR_TRANSITION_NAME));
        shadowView.setTransitionName(animationBundle.getString(EXTRAS_SHADOW_TRANSITION_NAME));
        masterTitleTextSize = animationBundle.getFloat(EXTRAS_SHARED_TITLE_TEXT_SIZE);
        masterAuthorTextSize = animationBundle.getFloat(EXTRAS_SHARED_AUTHOR_TEXT_SIZE);

        setTransitionListener(getEnterTransitionListener());
    }

    private void setupViewModel() {
        BookViewModel viewModel = ViewModelProviders
                .of(this, viewModelFactory)
                .get(BookViewModel.class);

        viewModel.getBook(bookId)
                .observe(this, resource -> {
                    if ((resource != null) && (resource.getStatus() == SUCCESS)) {
                        setupBookViews(resource.getData());
                    }
                });
    }

    private void setupBookViews(Book book) {
        if (book == null) {
            return;
        }

        tvExpandedTitle.setText(book.getTitle());
        tvCollpsedTitle.setText(book.getTitle());
        collapsingToolbarLayout.setTitle(/*book.getTitle()*/" ");
        authorTextView.setText(book.getAuthor());
        categoryTextView.setText(book.getCategory().toString());
        descriptionTextView.setText(book.getDescription());

        if (book.getImageUrl() != null) {
            loadCover(book.getImageUrl());
        }
    }

    private void loadCover(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new BitmapImageViewTarget(imageView) {
                    @Override
                    public void onResourceReady(Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        super.onResourceReady(resource, transition);
                        setupBarColors(resource);
                    }
                });
    }

    private void setupBarColors(Bitmap bitmap) {
        Palette.Swatch swatch = getDominantColor(bitmap);

        if (swatch != null) {
            collapsingToolbarLayout.setContentScrimColor(getActionBarColorFromSwatch(swatch));
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getStatusBarColorFromSwatch(swatch));
        }
    }

    private void setupAppBarLayoutOffsetListener() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int linesCount = tvExpandedTitle.getLineCount();
            if (this.lastAppBarOffset != verticalOffset || linesCount != this.linesCount) {
                this.lastAppBarOffset = verticalOffset;
                this.linesCount = linesCount;

                int appBarScrollRange = appBarLayout.getTotalScrollRange();
                appBarScrollPercentValue = (double) (appBarScrollRange + verticalOffset) / (double) appBarScrollRange;

                float collapsedTextSize = getResources().getDimension(R.dimen.collapsed_toolbar_text_size);
                float expandTextSize = getResources().getDimension(R.dimen.expanded_toolbar_text_size);
                float textSizeDifference = expandTextSize - collapsedTextSize;
                float textSize = collapsedTextSize + (float) (textSizeDifference * appBarScrollPercentValue);
                tvExpandedTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                tvCollpsedTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);


                int expandedAlpha;
                int collapsedAlpha;
                if (appBarScrollPercentValue > APP_BAR_MIN_EXPANDED_SCROLL_PERCENT_VALUE) {
                    expandedAlpha = MAX_ALPHA;
                    collapsedAlpha = MIN_ALPHA;
                } else if (appBarScrollPercentValue < APP_BAR_MAX_COLLEPSED_SCROLL_PERCENT_VALUE) {
                    expandedAlpha = MIN_ALPHA;
                    collapsedAlpha = MAX_ALPHA;
                } else {
                    double aplhaPercent = (appBarScrollPercentValue - APP_BAR_MAX_COLLEPSED_SCROLL_PERCENT_VALUE) / APP_BAR_ALPHA_SCROLL_MAX_PERCENT_VALUE;
                    expandedAlpha = (int) ((double) MAX_ALPHA * aplhaPercent);
                    collapsedAlpha = (int) ((double) MAX_ALPHA * (1.0 - aplhaPercent));
                }
                tvExpandedTitle.setTextColor(ColorUtils.setAlphaComponent(Color.WHITE, expandedAlpha));
                tvCollpsedTitle.setTextColor(ColorUtils.setAlphaComponent(Color.WHITE, collapsedAlpha));

                int bottomCollapsedSpacing = getResources().getDimensionPixelSize(R.dimen.collapsed_toolbar_bottom_spacing);
                int bottomExpandedSpacing = getResources().getDimensionPixelSize(R.dimen.expanded_toolbar_bottom_spacing);
                int bottomSpacingDifference = bottomExpandedSpacing - bottomCollapsedSpacing;
                int bottomSpacing = bottomCollapsedSpacing + (int) (bottomSpacingDifference * appBarScrollPercentValue);
                ViewGroup.MarginLayoutParams clLayoutParams = (ViewGroup.MarginLayoutParams) clTitle.getLayoutParams();
                if (linesCount > 1) {
                    int lineHeight = tvExpandedTitle.getLineHeight();
                    double lineDistance = lineHeight * (linesCount - 1);
                    int titleNegativeSpacing = (int) (lineDistance * (1.0 - appBarScrollPercentValue));

                    clLayoutParams.bottomMargin = -titleNegativeSpacing;
                } else {
                    clLayoutParams.bottomMargin = 0;
                }
                clTitle.requestLayout();

                int endCollapsedSpacing = getResources().getDimensionPixelSize(R.dimen.toolbar_item_size);
                int horizontalExpandedSpacing = getResources().getDimensionPixelSize(R.dimen.spacing_normal);

                double endSpacingDifference = /*horizontalExpandedSpacing +*/ endCollapsedSpacing;
                int endSpacing = /*endCollapsedSpacing*/horizontalExpandedSpacing + (int) (endSpacingDifference * (1.0 - appBarScrollPercentValue));


                ViewGroup.MarginLayoutParams tvExpandedTitleLayoutParams = (ViewGroup.MarginLayoutParams) tvExpandedTitle.getLayoutParams();
                tvExpandedTitleLayoutParams.bottomMargin = bottomSpacing;

                /*
                * "layoutParams.setMarginEnd(endSpacing);"
                * is not working :(
                */
                boolean isRTL = getResources().getBoolean(R.bool.is_layout_direction_rtl);
                if (isRTL) {
                    tvExpandedTitleLayoutParams.leftMargin = endSpacing;
                } else {
                    tvExpandedTitleLayoutParams.rightMargin = endSpacing;
                }

                tvExpandedTitle.requestLayout();
            }
        });
    }
}
