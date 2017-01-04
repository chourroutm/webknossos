import _ from "lodash";
import Backbone from "backbone";
import moment from "moment";

class LoggedTimeCollection extends Backbone.Collection {

  comparator(model) { return -model.get("interval"); }

  url() {
    if (this.userID) {
      return `/api/users/${this.userID}/loggedTime`;
    } else {
      return "/api/user/loggedTime";
    }
  }

  initialize(models, options) {
    return this.userID = options.userID;
  }


  parse(response) {
    return response.loggedTime.map(
      (entry) => {
        const interval = entry.paymentInterval;
        return {
          interval: moment(`${interval.year} ${interval.month}`, "YYYY MM"),
          time: moment.duration(entry.durationInSeconds, "seconds"),
          months: (interval.year * 12) + interval.month,
        };
      }).sort((a, b) => b.months - a.months);
  }
}

export default LoggedTimeCollection;
