var express = require('express');
var http = require('http');

var app = express();
var regids = [];

app.set('port', process.env.PORT || 3000);
app.use(express.bodyParser());

app.listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port') + '\n');
});

app.post('/', function(req, res){
    if (regids.indexOf(req.body.regid) === -1) {
        regids.push(req.body.regid);
    }
    
    var name = req.body.name.substring(0, req.body.name.indexOf('@'));
    
    setTimeout(function() {
        notify(name);
    }, 5000); 
});


function notify(name) { 
    var post_data = JSON.stringify({
        'data': {
            'title': name + ' subscribed :)\nEverybody say welcome !!!',
            'message': 'message'
        },
        'registration_ids': regids
    });
    
    var post_options = {
            host: 'android.googleapis.com',
            port: '80',
            path: '/gcm/send',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': post_data.length,
                'Authorization': 'key=AIzaSyD4bn8gfXfdH-Q55Bt0OrH5boqi_ahUT1Y'
            }
    };
    
    var post_req = http.request(post_options, function(res) {
        res.setEncoding('utf8');
        res.on('data', function (chunk) {
            console.log('Response: ' + chunk + '\n');
        });
    });
    
    // post the data
    post_req.write(post_data);
    post_req.end();
}
