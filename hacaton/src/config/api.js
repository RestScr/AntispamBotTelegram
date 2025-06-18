import {Urlapi} from "./config";

export const checkSession = (body) => {
    console.log(body);
    return fetch(`https://${Urlapi}/getAllData.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    });
};

export const updateFilter = (body) => {
    console.log(body);

    return fetch(`https://${Urlapi}/addFilter.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    })
}
export const removeFilter = (body) => {
    console.log(body);

    return fetch(`https://${Urlapi}/removeFilter.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    })
}
export const deleteUser = (body) => {
    console.log(body);
    return fetch(`https://${Urlapi}/removeFromWhitelist.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    })
}

export const loadLog = (body) => {
    console.log(body);
    return fetch(`https://${Urlapi}/getBotLogs.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    })
}
export const joinWhiteList = (body) => {
    console.log(body);
    return fetch(`https://${Urlapi}/addToWhitelist.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    })
}
export const cheackValid = (body) => {
    return fetch(`https://${Urlapi}/checkToken.php`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(body),
    });
};

