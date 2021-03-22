[![](https://jitpack.io/v/ngtien137/AndroidPdfView.svg)](https://jitpack.io/#ngtien137/AndroidPdfView)
# AndroidPdfView
Android Pdf Reader with PDFRenderer Android (API >=21)
## Preview 
![alt text](https://github.com/ngtien137/AndroidPdfView/blob/master/resource/preview.gif) 
![alt text](https://github.com/ngtien137/AndroidPdfView/blob/master/resource/img_demo.png) 
### Configure build.gradle (Project)
* Add these lines:
```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
### Configure build gradle (Module):
* Import module base:
```gradle
dependencies {
  implementation 'com.github.ngtien137:AndroidPdfView:TAG'
}
```
* You can get version of this module [here](https://jitpack.io/#ngtien137AndroidPdfView)
## All Attributes 
* Components:
![alt text](https://github.com/ngtien137/AndroidPdfView/blob/master/resource/img_demo.png) 
* All attributes
```xml
<declare-styleable name="AndroidPdfView">
  <attr name="apdf_page_margin" format="dimension" />
  <attr name="apdf_page_boundaries" format="dimension" />
  <attr name="apdf_vertical_page_extra_margin_horizontal" format="dimension" />
  <attr name="apdf_horizontal_divider_for_page" format="boolean" />

  <attr name="apdf_thumbnail_shadow_color" format="color" />

  <attr name="apdf_seekbar_visible_mode">
      <enum name="hidden" value="-1" />
      <enum name="visible" value="0" />
      <enum name="visible_when_scroll" value="1" />
  </attr>
</declare-styleable>
```
