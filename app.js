/**
 * Module dependencies.
 */

var express = require('express');
var http = require('http');
var path = require('path');

var app = express();

// all environments
app.set('port', process.env.PORT || 3000);
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');
app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.json());
app.use(express.urlencoded());
app.use(express.methodOverride());
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' === app.get('env')) {
  app.use(express.errorHandler());
}


http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});

app.use(express.bodyParser());

app.post('/', function(req, res){
    var count = 1;
    setInterval(function() {
        SayHey(req.body.regid, req.body.name, count);
        count += 1;
    }, 10000); 
});

//APA91bHCVTF_nDeo58PudKU_urBG6D-Sy7cv3lPbMMUe7MnYSUnSYvXH-GRzmkobja7rCMoJSsHjfw9tAZ932d7rOYj-CfUsBMFE1toOXI8_k1NEq653CB3AtAwRBy__aaullSYcT7SBJh9zMntoLbn-kZUV88kC5Hcubx5z0dmU_xX0M1WWw1Q

function SayHey(regid, name, count) { 
    var post_data = JSON.stringify({
        "data": {
            "title": "hey " + name + "! " + count,
            "message": "message"
        },
        "registration_ids": [regid]
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
            console.log('Response: ' + chunk);
        });
    });
    
    // post the data
    post_req.write(post_data);
    post_req.end();
}
