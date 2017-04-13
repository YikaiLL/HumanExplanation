$('#search_submit').click(function (event) {
    event.preventDefault();
    var item = {
        query: $('#search_box').val(),
        csrfmiddlewaretoken: $('input[name="csrfmiddlewaretoken"]').val()
    }
    console.log('hi');
    console.log(item.query);

    $.ajax({
        url: "search/", // the endpoint
        type: "POST", // http method
        data: item, // data sent with the post request
        // handle a successful response
        success: function (data) {
            $('#search_box').val(''); // remove the value from the input
            var json = JSON.parse(data);
            console.log(json);
            $(".history").remove();
            len = json.searchbox.length;
            console.log(len);
            if (len > 0) {
                for (var i = 0; i < len; i++) {
                    $("#search_result").prepend("<li class='history'>" +
                        "<a target='_blank' href=" + json.searchbox[i].canonicalUrl + ">" + json.searchbox[i].name + "</a>" +
                        "</li>");
                }
            } else {
                $('#search_result').prepend("<p class='history'>Ops...result not found</p>");
            }
            console.log("success"); // another sanity check
        },

        // handle a non-successful response
        error: function (xhr, errmsg, err) {
            console.log('error'); // provide a bit more info about the error to the console
        }
    });
});

