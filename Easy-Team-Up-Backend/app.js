const mysql = require('mysql2')
const express = require('express')
var sha256 = require('js-sha256');

const app = express()
//var log4js = require("log4js");
//var logger = log4js.getLogger();
app.use(express.json())
//app.use(log4js.connectLogger(logger, { level: log4js.levels.ERROR }));

// for server
const db = mysql.createConnection({
    host: '127.0.0.1',
    port: "3306",
    user: 'admin',
    password: 'easyteamup',
    database: 'easyteamup'
});

// for local
/*
const db = mysql.createConnection({
    host: '127.0.0.1',
    port: "3306",
    user: 'root',
    password: 'stevenli',
    database: 'EasyTeamUp'
});
*/

db.connect((err) => {
    if (err) {
        throw err
    } else {
        console.log('Connected to mySQL...')
    }
})

function add_notification(userId, occasion, eventName, info){
    db.query("insert into notifications values (?,?,?,?)", [userId, occasion, eventName, info], (err, result) => {
        if (err) {
            res.status(404).send()
        }
    });
}

function get_users(eventName){
    
}

// database specifics:
// - users: id, hashed_password, age, bio
// - events: name, description, ownerId, date, dueDate, publicity, location
// - eventPaticipants: name, participantId, joined

app.post('/login', (req, res) => {
    // TODO: hash password
    req.body.password = sha256(req.body.password)
    const params = [req.body.id, req.body.password]
    console.log(params)

    db.query('select * from users where id = ? and hashedPassword = ?', params, (err, result) => {
        if (result.length == 0) {
            res.status(404).send()
        }
        else {
            const object2Return = {
                id: result[0].id,
                age: result[0].age,
                bio: result[0].bio,
                image: result[0].image
            }
            console.log(object2Return)
            res.status(200).send(JSON.stringify(object2Return))
        }
    })
})

app.post('/signup', (req, res) => {
    // TODO: hash password
    req.body.password = sha256(req.body.password)
    const params = [req.body.id, req.body.password, req.body.age, req.body.bio, req.body.image]
    console.log(params);
    db.query('select * from users where id = ?', req.body.id, (err, result) => {
        console.log(result)
        if (result.length == 0) {
            db.query('insert into users values (?,?,?,?,?)', params, (err, result) => {
                res.status(201).send()
            })
        }
        else {
            res.status(400).send()
        }
    })
})

// user creates a event
app.post("/events", (req, res) => {
    const params = [req.body.name, req.body.description, req.body.ownerId, req.body.eventDate, req.body.duration, req.body.timeslots, req.body.dueDate, req.body.publicity, req.body.latitude, req.body.longitude, req.body.address]
    const eventName = req.body.name
    console.log(params)
    db.query('insert into events values (?,?,?,?,?,?,?,?,?,?,?)', params, (err, result) => {
        console.log(result)
        if (err) {
            res.status(404).send()
        }
        // insert invitees to participants table
        if (req.body.invitees != null) {
            JSON.parse(req.body.invitees).forEach(element => {
                db.query("insert into participants values (?,?,?)", [eventName, element, 0], (err, result) => {
                    if (err) {
                        res.status(404).send()
                    }
                });
                add_notification(element, "invite", eventName, req.body.ownerId);
            })
        }
        res.status(201).send();
    })
})

app.post("/update_event", (req, res) => {
    const params = [req.body.description, req.body.eventDate, req.body.duration, req.body.dueDate, req.body.publicity, req.body.latitude, req.body.longitude, req.body.address, req.body.name]
    db.query('UPDATE events SET description=?, eventDate=?, duration=?, dueDate=?, publicity=?, latitude=?, longitude=?, address=? WHERE name=?', params, (err, result) => {
        console.log(result);
        db.query("select userId from participants where status=1 and eventName=?", [req.body.name], (err, result1) => {
            console.log(result1);
            let set = new Set();
            result1.forEach(each =>{
                if(!set.has(each.userId)){
                    set.add(each.userId)
                    add_notification(each.userId, "change", req.body.name, "");
                }
            })
        });
	    res.status(201).send();

    })
})

// retrive all public events
app.get('/events', (req, res) => {
    db.query('select * from events', (err, result) => {
        res.status(200).send(JSON.stringify(result))
    })
})

app.get('/determined_times', (req, res) => {
    db.query('select startTimeCnt.eventName, startTimeCnt.startTime from (select eventName, startTime, count(userId) as cnt from voteTimeSlots group by eventName, startTime)startTimeCnt join (select eventName, max(cnt) as maxCnt from (select eventName, startTime, count(userId) as cnt from voteTimeSlots group by eventName, startTime)startTimeCnt2 group by eventName)startTimeMaxCnt on startTimeCnt.eventName=startTimeMaxCnt.eventName and startTimeCnt.cnt=startTimeMaxCnt.maxCnt', (err, result) => {
        res.status(200).send(JSON.stringify(result))
    })
})

// retreive invitations
app.get('/users/:id/invited_events', (req, res) => {
    db.query('select * from events where name in (select eventName from participants where userId = ? and status = 0)', req.params.id, (err, result) => {
        res.status(200).send(JSON.stringify(result));
    })
})

app.get('/users/:id/joined_events', (req, res) => {
    db.query('select * from events where name in (select eventName from participants where userId = ? and status = 1)', req.params.id, (err, result) => {
        res.status(200).send(JSON.stringify(result));
    })
})



// verify if a user exists
app.get('/users/:id', (req, res) => {
    db.query('select * from users where id = ?', req.params.id, (err, result) => {
        if (result.length == 0) {
            res.status(404).send()
        }
        else {
            res.status(200).send()
        }
    })
})

app.post("/signup_event", (req, res) => {
    console.log([req.body.eventName, req.body.userId, req.body.timeslots])
    db.query("insert into participants values (?,?,?)", [req.body.eventName, req.body.userId, 1], (err, result) => {
        if (err) {
            res.status(404).send()
        }
        add_notification(req.body.ownerId, "signup", req.body.eventName, req.body.userId)
    });
    if (req.body.timeslots != null) {
        JSON.parse(req.body.timeslots).forEach(element => {
            db.query("insert into voteTimeSlots values (?,?,?)", [req.body.eventName, req.body.userId, element], (err, result) => {
                if (err) {
                    res.status(404).send()
                }
            })
        })
    }
    res.status(201).send();

})

app.post("/withdraw_event", (req, res) => {
    console.log([req.body.eventName, req.body.userId])
    db.query("DELETE FROM participants WHERE eventName=? and userId=? and status=?", [req.body.eventName, req.body.userId, 1], (err, result) => {
        if (err) {
            res.status(404).send()
        }
        add_notification(req.body.ownerId, "withdraw", req.body.eventName, req.body.userId)
    });
    db.query("DELETE FROM voteTimeSlots WHERE eventName=? and userId=?", [req.body.eventName, req.body.userId], (err, result) => {
        if (err) {
            res.status(404).send()
        }
    })
    res.status(201).send();

})

app.post("/reject_event", (req, res) => {
    console.log([req.body.eventName, req.body.userId])
    db.query("DELETE FROM participants WHERE eventName=? and userId=? and status=?", [req.body.eventName, req.body.userId, 0], (err, result) => {
        if (err) {
            res.status(404).send()
        }
        add_notification(req.body.ownerId, "decline", req.body.eventName, req.body.userId)
    });
    db.query("insert into participants values (?,?,?)", [req.body.eventName, req.body.userId, -1], (err, result) => {
        if (err) {
            res.status(404).send()
        }
    });
    res.status(201).send();

})

app.post("/add_notification", (req, res) => {
    console.log([req.body.userId, req.body.occasion, req.body.eventName, req.body.info]);
    db.query("insert into notifications values (?,?,?,?)", [req.body.userId, req.body.occasion, req.body.eventName, req.body.info], (err, result) => {
        if (err) {
            res.status(404).send()
        }
    });
    res.status(201).send();
})

app.get("/get_notifications/:id", (req, res) => {
    db.query("select * from notifications where userId=?", [req.params.id], (err, result) => {
        if (err) {
            res.status(404).send()
        }
        res.status(201).send(JSON.stringify(result));
    });
    db.query("DELETE FROM notifications WHERE userId=?", [req.params.id], (err, result) => {
        if (err) {
            res.status(404).send()
        }
    });
})

app.post("/edit_profile", (req, res) => {
    const params = [req.body.age, req.body.bio, req.body.image, req.body.userId]
    db.query('UPDATE users SET age=?, bio=?, image=? WHERE id=?', params, (err, result) => {
        if (err) {
            res.status(404).send()
        }
	    res.status(201).send();
    })
})

app.listen(80, () => {
    console.log('Listening on port 80...')
})


