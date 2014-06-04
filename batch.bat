git add .
git commit -m "init"
git push -u origin master
lev.kazakov@gmail.com
git push heroku master
heroku ps:scale web=1
heroku ps
heroku logs --tail