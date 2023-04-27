import "test/mocks/lz4";
import path from "path";
import { compareScreenshot, isPixelEquivalent } from "./screenshot_helpers";
import {
  test,
  getNewPage,
  screenshotDatasetView,
  setupBeforeEachAndAfterEach,
  withRetry,
  checkBrowserstackCredentials,
} from "./dataset_rendering_helpers";

checkBrowserstackCredentials();

process.on("unhandledRejection", (err, promise) => {
  console.error("Unhandled rejection (promise: ", promise, ", reason: ", err, ").");
});
const BASE_PATH = path.join(__dirname, "../../../../frontend/javascripts/test/screenshots-wkorg");
const URL = "https://webknossos.org";

console.log(`[Info] Executing tests on URL ${URL}.`);
setupBeforeEachAndAfterEach();

const demoDatasetName = "l4dense_motta_et_al_demo";
const owningOrganization = "scalable_minds";

test.serial(`it should render dataset ${demoDatasetName} correctly`, async (t) => {
  await withRetry(
    3,
    async () => {
      const datasetId = {
        name: demoDatasetName,
        owningOrganization,
      };
      const { screenshot, width, height } = await screenshotDatasetView(
        await getNewPage(t.context.browser),
        URL,
        datasetId,
      );
      const changedPixels = await compareScreenshot(
        screenshot,
        width,
        height,
        BASE_PATH,
        demoDatasetName,
      );
      return isPixelEquivalent(changedPixels, width, height);
    },
    (condition) => {
      t.true(
        condition,
        `Dataset with name: "${demoDatasetName}" does not look the same, see ${demoDatasetName}.diff.png for the difference and ${demoDatasetName}.new.png for the new screenshot.`,
      );
    },
  );
});
