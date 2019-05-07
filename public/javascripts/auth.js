
{
    authUser = () => new Promise((resolve, reject) => {
        let email = $('#login_email').val();
        let password = $('#login_password').val();

        if (email && password) $.ajax({
            type: 'POST',
            url: "https://www.jeremydowens.com/authenticate",
            data: JSON.stringify({
                email,
                password
            }),
            contentType: 'application/json',
            success: s => {
                if (s.success)
                    $('#loginmessage').text(s.success);
                resolve(true);
            },
            error: e => {
                reject(false);
            },
        });
    });

    createUser = () => new Promise((resolve, reject) => {
        let email = $('#create_acct_email').val();
        let uname = $('#create_acct_uname').val();

        if (email && uname) $.ajax({
            type: 'POST',
            url: "https://www.jeremydowens.com/createaccount",
            data: JSON.stringify({
                email,
                uname
            }),
            contentType: 'application/json',
            success: s => {
                if (s.success)
                    $('#resultmessage').text(s.success);
                resolve(true);
            },
            error: e => {
                reject(false);
            },
        });
    });

    $(document).ready(function() {
        $('#acctcreate').on('click', function() {
            $('#spinner').show();
            createUser().then(() => {
                $('#spinner').hide();
            });
        });
        $('#acctlogin').on('click', function() {
            $('#spinner').show();
            authUser().then(() => {
                $('#spinner').hide();
                window.location.replace("/");
            });
        });
    });
}