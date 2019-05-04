{
    let email = $('#create_acct_email').val();
    $.ajax({
        type: 'POST',
        url: "https://www.jeremydowens.com/createaccount",
        data: JSON.stringify({
            email
        }),
        contentType: 'application/json',
        success: s => {
            if (s.result) $('#resultmessage').text(s.result)
        },
        error: e => {

        }
    })
}