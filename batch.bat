git add .
git commit -m "init"
git push -u origin master
git push heroku master
heroku ps:scale web=1
heroku ps
heroku logs --tail