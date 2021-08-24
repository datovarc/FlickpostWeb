$(function(){

  //Flash settings
  $('.alert-flash').delay(5000).fadeOut('slow');

  //Tooltip initiate
  $('[data-toggle="tooltip"]').tooltip();

  //Adding ripple effect on buttons
  $(".btn").click(function (e) {

    // Remove any old one
    $(".ripple").remove();

    // Setup
    var posX = $(this).offset().left,
        posY = $(this).offset().top,
        buttonWidth = $(this).width(),
        buttonHeight =  $(this).height();

    // Add the element
    $(this).prepend("<span class='ripple'></span>");


   // Make it round!
    if(buttonWidth >= buttonHeight) {
      buttonHeight = buttonWidth;
    } else {
      buttonWidth = buttonHeight;
    }

    // Get the center of the element
    var x = e.pageX - posX - buttonWidth / 2;
    var y = e.pageY - posY - buttonHeight / 2;


    // Add the ripples CSS and start the animation
    $(".ripple").css({
      width: buttonWidth,
      height: buttonHeight,
      top: y + 'px',
      left: x + 'px'
    }).addClass("rippleEffect");
  });

  //Configure countries
  if ($(".countries").length > 0) {
    $(".countries").each(function() {
      var type = $(this).data('key');
      var selectInput = $(this);
      $.ajax({
        url: "/getcountries/"+type,
        cache: false,
        success: function(data){
          if(data.countries){
            selectInput.find('option').remove();
            selectInput.append('<option value="">Select Country</option>');
            $.each(data.countries, function(k, v) {
               selectInput.append('<option value="'+v+'">'+v+'</option>');
             });
          }
        }
      });
    });
  }

});
