
{
    const authUser = () => new Promise((resolve, reject) => {
        let email = $('#login_email').val();
        let password = $('#login_password').val();

        if (email && password) $.ajax({
            type: 'POST',
            url: "/authenticate",
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

    const createUser = () => new Promise((resolve, reject) => {
        let email = $('#create_acct_email').val();
        let uname = $('#create_acct_uname').val();

        if (email && uname) $.ajax({
            type: 'POST',
            url: "/createaccount",
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
            $('#create_spinner').show();
            createUser().then(() => {
                $('#create_spinner').hide();
            });
        });
        $('#acctlogin').on('click', function() {
            $('#login_spinner').show();
            authUser().then(() => {
                $('#login_spinner').hide();
                window.location.replace("/");
            });
        });
    });
}