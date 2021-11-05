function openModal(imageInfo) {
    maxImages = imageInfo.length;
    var allImages = document.getElementsByClassName("dynamicImages")[0];
    allImages.innerHTML = '';
    var allThumbs = document.getElementsByClassName("dynamicThumbs")[0];
    allThumbs.innerHTML = '';


    for(let i=0; i<imageInfo.length; i++){
        var lightboxImageDiv = document.createElement('div');
        lightboxImageDiv.className = "lightboxImages";

        var numberText = document.createElement('div');
        numberText.className = "numbertext";
        numberText.innerHTML = (i+1)+'/'+maxImages;

        var lightboxImage = document.createElement('img');
        lightboxImage.src = imageInfo[i].path;
        lightboxImage.style = "width:85%;margin: auto;";

        lightboxImageDiv.appendChild(numberText);
        lightboxImageDiv.appendChild(lightboxImage);

        allImages.appendChild(lightboxImageDiv);

        var lightboxThumbDiv = document.createElement('div');
        lightboxThumbDiv.className = "lightboxThumb";

        var lightboxThumb = document.createElement('img');
        lightboxThumb.className = "thumb";
        lightboxThumb.src = imageInfo[i].path;
        lightboxThumb.onclick = function(){currentSlide(i)};
        lightboxThumb.alt = imageInfo[i].status;
        lightboxThumb.style = "width:100%;";

        lightboxThumbDiv.appendChild(lightboxThumb);
        allThumbs.appendChild(lightboxThumbDiv);

    }

    document.getElementById("lightbox").style.display = "block";
    showSlides(slideIndex = 0);
    showThumbs(thumbIndex = 0);
}

function closeModal() {
    var allImages = document.getElementsByClassName("dynamicImages")[0];
    allImages.innerHTML = '';
    var allThumbs = document.getElementsByClassName("dynamicThumbs")[0];
    allThumbs.innerHTML = '';
    document.getElementById("lightbox").style.display = "none";
    document.getElementsByClassName("addImages")[0].style.display = "none";
}

var slideIndex = 0;
showSlides(slideIndex);

var maxImages = 0;
var thumbIndex = 0;
showThumbs(thumbIndex);

function plusSlides(n) {
    var thumbMax = 5;
    showSlides(slideIndex += n);
    if(slideIndex >= (thumbIndex + thumbMax)){
        showThumbs(thumbIndex+=1);
    } else {
        showThumbs(thumbIndex=slideIndex)
    }
}

function plusThumbs(n) {
    showThumbs(thumbIndex += n);
}

function currentSlide(n) {
    showSlides(slideIndex = n);
}

function showSlides(n) {
    var i;
    var slides = document.getElementsByClassName("lightboxImages");
    var thumbs = document.getElementsByClassName("thumb");
    var captionText = document.getElementById("caption");
    if (n > slides.length-1) {slideIndex = 0}
    if (n < 0) {slideIndex = slides.length-1}
    for (i = 0; i < slides.length; i++) {
        slides[i].style.display = "none";
    }
    for (i = 0; i < thumbs.length; i++) {
        thumbs[i].className = thumbs[i].className.replace(" active", "");
    }
    slides[slideIndex].style.display = "block";
    thumbs[slideIndex].className += " active";
    captionText.innerHTML = thumbs[slideIndex].alt;
}

function showThumbs(n) {
    var i;
    var thumbMax = 5;
    var thumbs = document.getElementsByClassName("lightboxThumb");
    if (n > maxImages-thumbMax) {thumbIndex = maxImages-thumbMax}
    if (n < 0) {thumbIndex = 0}
    for (i = 0; i < thumbs.length; i++) {
        thumbs[i].style.display = "none";
    }
    for (i = 0; i < thumbMax; i++) {
        if(thumbs[thumbIndex+i] != null){
            thumbs[thumbIndex+i].style.display = "block";
        }
    }

    var addImages = document.getElementsByClassName("addImages")[0];
    addImages.style.display = "block";
}