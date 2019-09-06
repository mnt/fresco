/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.factory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.facebook.animated.gif.GifImage;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TestNativeLoader;
import com.facebook.imagepipeline.testing.TrivialBufferPooledByteBuffer;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AnimatedImageFactory} */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({
  GifImage.class,
  AnimatedImageFactoryImpl.class,
  AnimatedImageCompositor.class
})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
public class AnimatedImageFactoryGifImplTest {

  private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  @Rule public PowerMockRule rule = new PowerMockRule();

  static {
    TestNativeLoader.init();
  }

  private static ResourceReleaser<PooledByteBuffer> FAKE_RESOURCE_RELEASER =
      new ResourceReleaser<PooledByteBuffer>() {

        @Override
        public void release(PooledByteBuffer value) {}
      };

  private static ResourceReleaser<Bitmap> FAKE_BITMAP_RESOURCE_RELEASER =
      new ResourceReleaser<Bitmap>() {

        @Override
        public void release(Bitmap value) {}
      };

  private AnimatedDrawableBackendProvider mMockAnimatedDrawableBackendProvider;
  private PlatformBitmapFactory mMockBitmapFactory;
  private AnimatedImageFactory mAnimatedImageFactory;

  private GifImage mGifImageMock;

  @Before
  public void setup() {
    PowerMockito.mockStatic(GifImage.class);
    mGifImageMock = mock(GifImage.class);

    mMockAnimatedDrawableBackendProvider = mock(AnimatedDrawableBackendProvider.class);
    mMockBitmapFactory = mock(PlatformBitmapFactory.class);

    mAnimatedImageFactory =
        new AnimatedImageFactoryImpl(mMockAnimatedDrawableBackendProvider, mMockBitmapFactory);

    ((AnimatedImageFactoryImpl) mAnimatedImageFactory).sGifAnimatedImageDecoder = mGifImageMock;
  }

  @Test
  public void testCreateDefaultsUsingPointer() {
    GifImage mockGifImage = mock(GifImage.class);

    // Expect a call to GifImage.create
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(mGifImageMock.decode(byteBuffer.getNativePtr(), byteBuffer.size()))
        .thenReturn(mockGifImage);

    testCreateDefaults(mockGifImage, byteBuffer);
  }

  @Test
  public void testCreateDefaultsUsingByteBuffer() {
    GifImage mockGifImage = mock(GifImage.class);

    // Expect a call to GifImage.create
    TrivialBufferPooledByteBuffer byteBuffer = createDirectByteBuffer();
    when(mGifImageMock.decode(byteBuffer.getByteBuffer())).thenReturn(mockGifImage);

    testCreateDefaults(mockGifImage, byteBuffer);
  }

  @Test
  public void testCreateWithPreviewBitmapUsingPointer() throws Exception {
    GifImage mockGifImage = mock(GifImage.class);
    Bitmap mockBitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to WebPImage.create
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(mGifImageMock.decode(byteBuffer.getNativePtr(), byteBuffer.size()))
        .thenReturn(mockGifImage);
    when(mockGifImage.getWidth()).thenReturn(50);
    when(mockGifImage.getHeight()).thenReturn(50);

    testCreateWithPreviewBitmap(mockGifImage, mockBitmap, byteBuffer);
  }

  @Test
  public void testCreateWithPreviewBitmapUsingByteBuffer() throws Exception {
    GifImage mockGifImage = mock(GifImage.class);
    Bitmap mockBitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to WebPImage.create
    TrivialBufferPooledByteBuffer byteBuffer = createDirectByteBuffer();
    when(mGifImageMock.decode(byteBuffer.getByteBuffer())).thenReturn(mockGifImage);
    when(mockGifImage.getWidth()).thenReturn(50);
    when(mockGifImage.getHeight()).thenReturn(50);

    testCreateWithPreviewBitmap(mockGifImage, mockBitmap, byteBuffer);
  }

  @Test
  public void testCreateWithDecodeAlFramesUsingPointer() throws Exception {
    GifImage mockGifImage = mock(GifImage.class);

    Bitmap mockBitmap1 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);
    Bitmap mockBitmap2 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to GifImage.create
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(mGifImageMock.decode(byteBuffer.getNativePtr(), byteBuffer.size()))
        .thenReturn(mockGifImage);
    when(mockGifImage.getWidth()).thenReturn(50);
    when(mockGifImage.getHeight()).thenReturn(50);

    testCreateWithDecodeAlFrames(mockGifImage, mockBitmap1, mockBitmap2, byteBuffer);
  }

  @Test
  public void testCreateWithDecodeAlFramesUsingByteBuffer() throws Exception {
    GifImage mockGifImage = mock(GifImage.class);

    Bitmap mockBitmap1 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);
    Bitmap mockBitmap2 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to GifImage.create
    TrivialBufferPooledByteBuffer byteBuffer = createDirectByteBuffer();
    when(mGifImageMock.decode(byteBuffer.getByteBuffer())).thenReturn(mockGifImage);
    when(mockGifImage.getWidth()).thenReturn(50);
    when(mockGifImage.getHeight()).thenReturn(50);

    testCreateWithDecodeAlFrames(mockGifImage, mockBitmap1, mockBitmap2, byteBuffer);
  }

  private void testCreateDefaults(GifImage mockGifImage, PooledByteBuffer byteBuffer) {
    EncodedImage encodedImage =
        new EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER));
    encodedImage.setImageFormat(ImageFormat.UNKNOWN);

    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage)
            mAnimatedImageFactory.decodeGif(
                encodedImage, ImageDecodeOptions.defaults(), DEFAULT_BITMAP_CONFIG);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockGifImage, imageResult.getImage());
    assertNull(imageResult.getPreviewBitmap());
    assertFalse(imageResult.hasDecodedFrame(0));

    // Should not have interacted with these.
    verifyZeroInteractions(mMockAnimatedDrawableBackendProvider);
    verifyZeroInteractions(mMockBitmapFactory);
  }

  private void testCreateWithPreviewBitmap(
      GifImage mockGifImage, Bitmap mockBitmap, PooledByteBuffer byteBuffer) throws Exception {
    // For decoding preview frame, expect some calls.
    final AnimatedDrawableBackend mockAnimatedDrawableBackend =
        createAnimatedDrawableBackendMock(1);
    when(mMockAnimatedDrawableBackendProvider.get(
            any(AnimatedImageResult.class), isNull(Rect.class)))
        .thenReturn(mockAnimatedDrawableBackend);
    when(mMockBitmapFactory.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap, FAKE_BITMAP_RESOURCE_RELEASER));
    AnimatedImageCompositor mockCompositor = mock(AnimatedImageCompositor.class);
    PowerMockito.whenNew(AnimatedImageCompositor.class)
        .withAnyArguments()
        .thenReturn(mockCompositor);

    ImageDecodeOptions imageDecodeOptions =
        ImageDecodeOptions.newBuilder().setDecodePreviewFrame(true).build();
    EncodedImage encodedImage =
        new EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER));
    encodedImage.setImageFormat(ImageFormat.UNKNOWN);
    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage)
            mAnimatedImageFactory.decodeGif(
                encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockGifImage, imageResult.getImage());
    assertNotNull(imageResult.getPreviewBitmap());
    assertFalse(imageResult.hasDecodedFrame(0));

    // Should not have interacted with these.
    verify(mMockAnimatedDrawableBackendProvider)
        .get(any(AnimatedImageResult.class), isNull(Rect.class));
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verify(mMockBitmapFactory).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG);
    verifyNoMoreInteractions(mMockBitmapFactory);
    verify(mockCompositor).renderFrame(0, mockBitmap);
  }

  private void testCreateWithDecodeAlFrames(
      GifImage mockGifImage, Bitmap mockBitmap1, Bitmap mockBitmap2, PooledByteBuffer byteBuffer)
      throws Exception {
    // For decoding preview frame, expect some calls.
    final AnimatedDrawableBackend mockAnimatedDrawableBackend =
        createAnimatedDrawableBackendMock(2);

    when(mMockAnimatedDrawableBackendProvider.get(
            any(AnimatedImageResult.class), isNull(Rect.class)))
        .thenReturn(mockAnimatedDrawableBackend);

    when(mMockBitmapFactory.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap1, FAKE_BITMAP_RESOURCE_RELEASER))
        .thenReturn(CloseableReference.of(mockBitmap2, FAKE_BITMAP_RESOURCE_RELEASER));
    AnimatedImageCompositor mockCompositor = mock(AnimatedImageCompositor.class);
    PowerMockito.whenNew(AnimatedImageCompositor.class)
        .withAnyArguments()
        .thenReturn(mockCompositor);

    ImageDecodeOptions imageDecodeOptions =
        ImageDecodeOptions.newBuilder()
            .setDecodePreviewFrame(true)
            .setDecodeAllFrames(true)
            .build();

    EncodedImage encodedImage =
        new EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER));
    encodedImage.setImageFormat(ImageFormat.UNKNOWN);

    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage)
            mAnimatedImageFactory.decodeGif(
                encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockGifImage, imageResult.getImage());
    assertNotNull(imageResult.getDecodedFrame(0));
    assertNotNull(imageResult.getDecodedFrame(1));
    assertNotNull(imageResult.getPreviewBitmap());

    // Should not have interacted with these.
    verify(mMockAnimatedDrawableBackendProvider)
        .get(any(AnimatedImageResult.class), isNull(Rect.class));
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verify(mMockBitmapFactory, times(2)).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG);
    verifyNoMoreInteractions(mMockBitmapFactory);
    verify(mockCompositor).renderFrame(0, mockBitmap1);
    verify(mockCompositor).renderFrame(1, mockBitmap2);
  }

  private TrivialPooledByteBuffer createByteBuffer() {
    byte[] buf = new byte[16];
    return new TrivialPooledByteBuffer(buf);
  }

  private static TrivialBufferPooledByteBuffer createDirectByteBuffer() {
    byte[] buf = new byte[16];
    return new TrivialBufferPooledByteBuffer(buf);
  }

  /**
   * Creates the mock for the AnimatedDrawableBackend with the number of frame
   *
   * @param frameCount The number of frame to mock
   */
  private AnimatedDrawableBackend createAnimatedDrawableBackendMock(final int frameCount) {
    // For decoding preview frame, expect some calls.
    final AnimatedDrawableBackend mockAnimatedDrawableBackend = mock(AnimatedDrawableBackend.class);
    when(mockAnimatedDrawableBackend.getFrameCount()).thenReturn(frameCount);
    when(mockAnimatedDrawableBackend.getWidth()).thenReturn(50);
    when(mockAnimatedDrawableBackend.getHeight()).thenReturn(50);
    return mockAnimatedDrawableBackend;
  }
}
